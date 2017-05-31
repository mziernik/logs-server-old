
------------ aktualizacja kolumny logs.values z tabeli attributes ---------------------------
WITH attrs AS ( 
  SELECT DISTINCT unnest(values) AS values, logs_id  
  FROM logs.attributes
  ORDER BY values
), 
tbl as (
  SELECT array_agg(values) AS values, logs_id 
  FROM attrs 
  GROUP BY logs_id
)
UPDATE logs.logs l 
SET values = tbl.values
FROM tbl
WHERE tbl.logs_id = l.logs_id

-----------------------------------------------------------------


INSERT INTO logs.values2 (hash, attr_type, value, ref_count)(
	WITH attrs AS (
		SELECT attr_type, unnest(values) as values_id, count(1) as ref_count 
		FROM logs.attributes 
		GROUP by attr_type, values_id	
	) SELECT hash, attr_type, value, ref_count	
	FROM logs.values 
	JOIN attrs using(values_id)
);




-------------- zrodlo --------------------
INSERT INTO logs.values2 (hash, attr_type, value, ref_count)(
	WITH attrs AS (
		SELECT count(1) as ref_count, 20 as attr_type, source as values_id 
		FROM logs.logs
		GROUP BY attr_type, values_id
	) SELECT hash, attr_type, value, ref_count 
	FROM logs.values 
	JOIN attrs using(values_id)
);

-------------- rodzaj --------------------
INSERT INTO logs.values2 (hash, attr_type, value, ref_count)(
	WITH attrs AS (
		SELECT count(1) AS ref_count, 7 as attr_type, kind as values_id
		FROM logs.logs
		GROUP BY attr_type, values_id
	) SELECT hash, attr_type, value, ref_count 
	FROM logs.values 
	JOIN attrs using(values_id)
);

