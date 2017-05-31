/*
	aktualizacja kolumny logs.all_values
*/

WITH attrs AS ( 
  SELECT DISTINCT unnest(values) AS all_values, logs_id  
  FROM logs.attributes
  ORDER BY all_values
), 
tbl as (
  SELECT array_agg(all_values) AS all_values, logs_id 
  FROM attrs 
  GROUP BY logs_id
)
UPDATE logs.logs l 
SET all_values = tbl.all_values
FROM tbl
WHERE tbl.logs_id = l.logs_id