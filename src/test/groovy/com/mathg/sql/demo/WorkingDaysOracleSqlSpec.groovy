package com.mathg.sql.demo

import groovy.sql.Sql
import groovy.time.TimeCategory
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import java.text.SimpleDateFormat

@Stepwise
class WorkingDaysOracleSqlSpec extends Specification {

    @Shared
    WorkingDaysOracleSql elements
    @Shared
    SimpleDateFormat formatter =  new SimpleDateFormat("dd/MM/yyyy")
    @Shared
    Sql sql

    def setupSpec() {
        elements = new WorkingDaysOracleSql()
        formatter.setTimeZone(TimeZone.getTimeZone("Europe/Paris")); // necessary for GregorianCalendar JDK-4651757
        Map conn = ['host': "host",
                    'port': "port",
                    'user': "user",
                    'pwd': "pwd",
                    'sid': "sid"]
        sql = Sql.newInstance( "jdbc:oracle:thin:@//${conn.host}:${conn.port}/${conn.sid}",
            "${conn.user}", "${conn.pwd}", "oracle.jdbc.OracleDriver")
    }

    @Unroll
    def """French working days list returned by the get_fr_working_days_list() function is correct for year#testedYear"""() {
        given:
        Closure getQueryForYear = { int year ->
            String testQuery = """WITH $elements.frHolidaysInList 
SELECT get_fr_holidays_list($year) AS value FROM dual"""
            testQuery
        }

        when:
        String list = sql.firstRow(getQueryForYear(testedYear)).getAt("VALUE")

        then:
        list == expectedList

        where:
        testedYear | expectedList
        2018       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '02/04', '10/05', '21/05'"
        2019       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '22/04', '30/05', '10/06'"
        2020       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '13/04', '21/05', '01/06'"
        2021       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '05/04', '13/05', '24/05'"
        2022       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '18/04', '26/05', '06/06'"
        2023       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '10/04', '18/05', '29/05'"
        2024       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '01/04', '09/05', '20/05'"
        2025       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '21/04', '29/05', '09/06'"
        2026       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '06/04', '14/05', '25/05'"
        2027       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '29/03', '06/05', '17/05'"
        2028       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '17/04', '25/05', '05/06'"
        2029       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '02/04', '10/05', '21/05'"
        2030       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '22/04', '30/05', '10/06'"
        2031       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '14/04', '22/05', '02/06'"
        2032       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '29/03', '06/05', '17/05'"
        2033       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '18/04', '26/05', '06/06'"
        2034       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '10/04', '18/05', '29/05'"
        2035       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '26/03', '03/05', '14/05'"
        2036       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '14/04', '22/05', '02/06'"
        2037       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '06/04', '14/05', '25/05'"
        2038       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '26/04', '03/06', '14/06'"
        2039       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '11/04', '19/05', '30/05'"
        2040       | "'01/01', '01/05', '08/05', '14/07', '15/08', '01/11', '11/11', '25/12', '02/04', '10/05', '21/05'"

    }

    int getTotalDaysBetween (Date start, Date end) {
        int diffJourDatesArg
        use (TimeCategory) {
            def diff =  start - end
            diffJourDatesArg = diff.getDays()
        }
        diffJourDatesArg
    }

    int getTotalMonthsBetween (Date start, Date end) {
        Calendar tStart = new GregorianCalendar()
        Calendar tEnd = new GregorianCalendar()
        tStart.setTime(start)
        def mStart = tStart.get(Calendar.MONTH)
        def yStart = tStart.get(Calendar.YEAR)
        tEnd.setTime(end)
        def mEnd = tEnd.get(Calendar.MONTH)
        def yEnd = tEnd.get(Calendar.YEAR)
        def diff = (yEnd - yStart)*12 + (mEnd - mStart)
        diff
    }

    @Unroll
    def """Test case : #testCase. The number of working days between dates #testDate (tested date) and #currentDate
        (simulated current date) is ok. Start date (tested date if past or simulated currente date if tested date is
        future) is always non inclusive whereas end date (the other date) is inclusive."""() {
        given:
        String aliasDateColumnToComputeWorkingDaysFor = "date_column"
        Date dateDay = formatter.parse(currentDate)
        Date dayTest = formatter.parse(testDate)
        int diffDaysNowDateDay = getTotalDaysBetween(new Date(), dateDay)
        int diffMonthsDateDayDateTest = Math.abs(getTotalMonthsBetween(dateDay, dayTest))
        int numberOfPastMonthsInWorkingDaysView
        int numberOfFutureMonthsInWorkingDaysView
        if (dayTest <= dateDay) {
            numberOfPastMonthsInWorkingDaysView = diffMonthsDateDayDateTest + 1
            numberOfFutureMonthsInWorkingDaysView = 0
        }
        else {
            numberOfPastMonthsInWorkingDaysView = 0
            numberOfFutureMonthsInWorkingDaysView = diffMonthsDateDayDateTest + 1
        }
        
        Closure getQueryForTestCase = { String testedDate, String simulatedCurrentDate ->
            String testQuery = """WITH ${elements.getWorkingDaysView(numberOfPastMonthsInWorkingDaysView, 
                numberOfFutureMonthsInWorkingDaysView, -diffDaysNowDateDay)}
                SELECT (SELECT $elements.numberOfWorkingDaysColumnAlias 
                FROM $elements.workingDaysViewName WHERE $elements.dayAlias = t.${aliasDateColumnToComputeWorkingDaysFor}) AS value
                FROM (SELECT to_date('$testedDate', 'dd/MM/yyyy') AS $aliasDateColumnToComputeWorkingDaysFor FROM dual) t"""
            println "testQuery : " + testQuery
            testQuery
        }

        when:
        int numberOfWorkingDays = sql.firstRow(getQueryForTestCase(testDate, currentDate)).getAt("VALUE")

        then:
        numberOfWorkingDays == expectedValue

        where:
        testCase                                         | testDate     | currentDate  | expectedValue
        "past date - start date is a working day"        | '05/11/2018' | '29/11/2018' | -18
        "past date - start date is saturday"             | '03/11/2018' | '28/11/2018' | -18
        "past date - start date is sunday"               | '04/11/2018' | '28/11/2018' | -18
        "past date - start date is friday"               | '02/11/2018' | '29/11/2018' | -19
        "past date - start date is thursday"             | '08/11/2018' | '29/11/2018' | -15
        "past date - start date is a fixed holiday"      | '01/11/2018' | '29/11/2018' | -20
        "past date - range contains a fixed holiday"     | '24/10/2018' | '29/11/2018' | -25
        "past date - range contains variable holidays"   | '03/03/2018' | '25/05/2018' | -55
        "future date - current date is a working day"    | '07/12/2018' | '03/12/2018' | 4
        "future date - current date is saturday"         | '22/12/2018' | '03/12/2018' | 14
        "future date - current date is sunday"           | '23/12/2018' | '03/12/2018' | 14
        "future date - current date is monday"           | '24/12/2018' | '03/12/2018' | 15
        "future date - current date is a fixed holiday"  | '01/11/2018' | '29/10/2018' | 2
        "future date - range contains a fixed holiday"   | '29/08/2018' | '31/07/2018' | 20
        "future date - range contains variable holidays" | '25/05/2018' | '03/03/2018' | 55
    }
}
