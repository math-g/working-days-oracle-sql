package com.mathg.sql.demo

import groovy.transform.CompileStatic

/**
 * Oracle SQL code elements used to compute the number of working days between a past or future date and the current date,
 * in "pure" SQL, without the need to use PL/SQL or to create any object in the schema.
 * Current date is by default sysdate, but can be forced to be an arbitrary date.
 * The date to compare to current date will typically be contained in a column.
 * The holidays computation part is specific to France, but with slight modifications, could be adapted to another country.
 * This rely on the functions in WITH clause feature, so Oracle 12.1 + is needed.
 * @author Mathieu Grelier
 */
@CompileStatic
class WorkingDaysOracleSql {

    /* exposed names or aliases */
    String frHolidaysInListFunctionName = "get_fr_holidays_list"
    String sysdateShiftedFunctionName = "sysdate_shifted"
    String workingDaysViewName = "liste_jours_ouvres"
    String dayAlias = "day"
    String numberOfWorkingDaysColumnAlias = "nb_jours_ouvres"

    /**
     * Oracle PL/SQL function returning holidays list for a given year as a comma separated list of 'dd/MM' values
     * of warchar2 type.
     * This is convenient when creating a TYPE is not an option, which would be a prerequisite to be able to create dynamic
     * in-lists, which in turn would be certainly be more idiomatic.
     * A varchar2 is returned so the client code will test a given day as belonging or not to the list with the LIKE operator.
     * Main difficulty is to be able to compute the variable national french holidays, which are all dependant of the Easter
     * sunday. This day is not the same from year to year and is computed from a quite complicated formula
     * (see https://fr.wikipedia.org/wiki/Calcul_de_la_date_de_P%C3%A2ques).
     * This is an original code, adapted from an Excel spreadsheet, and taking into account an Excel bug that consider
     * 1900 as a leap year.
     * Oracle 12.1+ is needed to be able to use PL/SQL function in SQL code.
     * @return Oracle PL/SQL Function providing comma separated day/month holidays list for a given year
     */
    String frHolidaysInList = """
	FUNCTION $frHolidaysInListFunctionName(year NUMBER) RETURN VARCHAR2
    AS  
        fixed_days VARCHAR2(100) := '''01/01'', ''01/05'', ''08/05'', ''14/07'', ''15/08'', ''01/11'', ''11/11'', ''25/12''';
        variable_days VARCHAR2(100) := '';
        minute NUMBER;
        day NUMBER;
        easter_sunday DATE;
    BEGIN
        SELECT (extract (minute from (cast(trunc(sysdate) + numtodsinterval(24*60*60 * ((year/38) - floor(year/38)), 'second') AS TIMESTAMP)))) / 2 +56 AS minute INTO minute FROM dual;
        IF floor(minute) = 60 THEN
            minute := 29;
        ELSIF minute > 60 THEN
            minute := minute - 1;
        END IF;
        SELECT extract (DAY FROM date'1899-12-31' + numtodsinterval(minute, 'DAY') ) AS day INTO day FROM dual;
        SELECT next_day(to_date(to_char(day) || '/05/' || to_char(year), 'dd/MM/yyyy'), 'SAMEDI') - 7 -interval '34' day AS day INTO easter_sunday FROM dual; -- 'SAMEDI' : saturday in fr locale
        variable_days := variable_days || ', ''' || to_char(easter_sunday + interval '1' day, 'dd/MM') || '''';
        variable_days := variable_days || ', ''' || to_char(easter_sunday + interval '39' day, 'dd/MM') || '''';
        variable_days := variable_days || ', ''' || to_char(easter_sunday + interval '50' day, 'dd/MM') || '''';
        RETURN fixed_days || variable_days;
    END;
    """

    /**
     * Oracle PL/SQL function used to return a specific date shifted from sysdate by a specific number of days.
     * Useful to simulate a sysdate in the past or the future.
     * @param dayOffset - positive or negative integer defining the offset to apply to sysdate
     * @return Oracle PL/SQL function returning a date shifted by a number of days equals to dayOffset
     */
    String getSysdateShifted(int dayOffset) {
        String sign = (dayOffset < 0) ? "-" : "+"

        """FUNCTION $sysdateShiftedFunctionName RETURN DATE
		IS  
		  selected_sysdate date;
		BEGIN
		  selected_sysdate := sysdate $sign ${dayOffset.abs()};
		  RETURN selected_sysdate;
		END;"""
    }

    /**
     * Oracle SQL code for a subquery factory returning a continuous date serie, giving for each day
     * the number of working days relative to current date.
     * This number of working days is the value obtained when :
     *  - starting from a past date, exclusive, each working day is counted until current date is reached, inclusive.
     *  - starting from current date, exclusive, each working day is counted until a future date is reached, inclusive.
     * In both cases, the time direction remains the same.
     * @param numberOfPastMonths - number of past months to include in the date serie, relative to current date
     * @param numberOfFutureMonths - number of future months to include in the date serie, relative to current date
     * @param dayOffset - if 0 (default), will use sysdate as current date, else, this argument is the offset, positive
     * or negative, to apply to sysdate to simulate a future or past current date. Used for specific business cases or
     * test cases.
     * Ex : use value -1 to simulate a current date equals to yesterday
     * @return String containing Oracle SQL code for a subquery factory creating a continuous date serie relative to
     * current date, giving for each date the corresponding number of working days.
     * @see #frHolidaysInList
     * @see #getSysdateShifted
     */
    String getWorkingDaysView (int numberOfPastMonths = 1,
                               int numberOfFutureMonths = 1,
                               int dayOffset = 0
    ) {
        boolean useSysdateShifted = (dayOffset != 0) ? true : false
        String sysdateFunction = (useSysdateShifted == true) ? "${sysdateShiftedFunctionName}()" : "sysdate"
        if (numberOfPastMonths < 0 || numberOfFutureMonths < 0) {
            throw new IllegalArgumentException("numberOfPastMonths and numberOfFutureMonths must be positive integers")
        }
        if (numberOfPastMonths == 0 && numberOfFutureMonths == 0) {
            throw new IllegalArgumentException("numberOfPastMonths and numberOfFutureMonths can't be both equal to zero")
        }
        // no UNION ALL to delete the duplicate first day of the serie (diff = 0)
        String unionClause = (numberOfPastMonths == 0 || numberOfFutureMonths == 0) ? "" : "UNION"

        String pastBlock = (numberOfPastMonths == 0) ? "" : """SELECT $dayAlias, diff, is_working_day, nb_days,
		CASE WHEN (is_working_day = 0) THEN (- SUM(is_working_day) OVER (ORDER BY $dayAlias desc)) ELSE (- SUM(is_working_day) OVER (ORDER BY $dayAlias desc)) + 1 END AS $numberOfWorkingDaysColumnAlias
        FROM (
            SELECT $dayAlias, diff, nb_days,
            CASE WHEN (to_char(trunc($dayAlias), 'D') IN (6, 7) OR  $frHolidaysInListFunctionName(extract (year from $dayAlias)) LIKE '%' || to_char(trunc($dayAlias), 'dd/MM') || '%') THEN 0 ELSE 1 END AS is_working_day
            FROM (
                SELECT add_months(trunc($sysdateFunction), -$numberOfPastMonths) + level AS $dayAlias,
                (add_months(trunc($sysdateFunction),-$numberOfPastMonths) + level) - trunc($sysdateFunction) AS diff,
                1-ROW_NUMBER() OVER (ORDER BY level desc) AS nb_days
                FROM dual
                CONNECT BY level < trunc($sysdateFunction) - add_months(trunc($sysdateFunction),-$numberOfPastMonths) + 1
            )
        )"""

        String futureBlock = (numberOfFutureMonths == 0) ? "" : """SELECT $dayAlias, diff, is_working_day, nb_days,
		(SUM(working_day_to_consider) OVER (ORDER BY $dayAlias)) AS $numberOfWorkingDaysColumnAlias
        FROM (
			SELECT $dayAlias, diff, is_working_day, nb_days,
			CASE WHEN (diff = 0) THEN 0 ELSE is_working_day END AS working_day_to_consider
			FROM (
				SELECT $dayAlias, diff, nb_days,
				CASE WHEN (to_char(trunc($dayAlias), 'D') IN (6, 7) OR $frHolidaysInListFunctionName(extract (year from $dayAlias)) LIKE '%' || to_char(trunc($dayAlias), 'dd/MM') || '%') THEN 0 ELSE 1 END AS is_working_day
				FROM (
					SELECT add_months(trunc($sysdateFunction), +$numberOfFutureMonths) - level as $dayAlias,
					add_months(trunc($sysdateFunction), $numberOfFutureMonths) - level - trunc($sysdateFunction) AS diff,
					ROW_NUMBER() OVER (ORDER BY level desc) - 1 AS nb_days
					FROM dual
					CONNECT BY level < add_months(trunc($sysdateFunction),$numberOfFutureMonths) + 1 -  trunc($sysdateFunction)
				)
			)
        )
        """

        String necessaryFunctions = frHolidaysInList
        if (useSysdateShifted) {
            necessaryFunctions  += "\n${getSysdateShifted(dayOffset)}"
        }


        return """$necessaryFunctions
        $workingDaysViewName AS (SELECT $dayAlias, is_working_day, diff, nb_days, $numberOfWorkingDaysColumnAlias FROM (
            $pastBlock
            $unionClause
            $futureBlock
        ) t0
        ORDER BY $dayAlias desc)
        """
    }

}
