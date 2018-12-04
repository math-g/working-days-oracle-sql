# Working days between current date and a date column in Oracle SQL (without any DDL)

Oracle SQL code elements used to compute the number of working days between a past or future date and the current date, in "pure" SQL, without the need to use PL/SQL or to create any object in the schema.

Current date is by default sysdate, but can be forced to be an arbitrary date.

The date to compare to current date will typically be contained in a column.

The holidays computation part is specific to France, but with slight modifications, could be adapted to another country.

This rely on the functions in WITH clause feature, so Oracle 12.1 + is needed.

See corresponding post :

[working-days-oracle-sql](https://math-g.github.io/2018/12/10/working-days-oracle-sql.html)