
--	DROP  TYPE logs.filter_packlet CASCADE
--	DROP  TYPE logs.filter_group  CASCADE

/*

CREATE TYPE logs.filter_item AS (
	attr_type int,
	operator int,
	value logs.lvalue
)

CREATE TYPE logs.filter_group AS (
	items logs.filter_item[] -- tablica elementów laczona jako OR
)


CREATE TYPE logs.filter_packet AS (
	 groups logs.filter_group[];  -- tablica elementów laczona jako AND	
)

*/

CREATE OR REPLACE FUNCTION logs.filter_logs(p_filters logs.filter_packet[]) 
RETURNS TABLE (logs_id integer) AS $$
DECLARE 
        id integer;
        hh integer;
        rec RECORD;
        hashes int[];
	vals int[];
	filter logs.filter_packlet;
BEGIN    
        IF p_filters IS NULL THEN 
                return;
        END IF;

        /**
		operator:
		1 - equals,
		2 - not equals,
		3 - like
		4 - not like
		5 - moreThan
		6 - lessThan
        */

		-- FOREACH attr IN ARRAY pck.attributes
	FOR i IN array_lower(p_filters, 1) .. array_upper(p_filters, 1)
	LOOP 
		filter := p_filters[i];

		IF filter.attr_type = 20 THEN

			

		END IF;
		


		RETURN QUERY SELECT filter.attr_type;

	END LOOP;

END;
$$ LANGUAGE plpgsql;


/*
SELECT * FROM logs.filter_logs(ARRAY[

	( 20, 1, ARRAY['Content', 'Rachunki'])

]::logs.filter_packlet[]);
*/




-------------- aktualne logi -----------------
--	SELECT * FROM logs.logs WHERE server_date > '2014-12-01'

/*   -------- wszystkie zrodla I RODZAJE -----------------
select * from logs.values where values_id in (
	SELECT DISTINCT source FROM logs.logs WHERE server_date > '2014-12-01'
)

*/




/*

WITH llogs AS (



	WITH pre AS (
		SELECT UNNEST (logs.find_values_arr(ARRAY['StorageImport-Dev', 'StorageImport-Demo', 'StorageImport-Test', 'StorageImport'])) AS source
	)

	SELECT distinct logs_id 
	FROM logs.logs 
	JOIN logs.values vsrc ON vsrc.values_id in (select source from pre)
	WHERE server_date > '2014-12-01'

	LIMIT 1000

)



SELECT 
	logs_id, 
	server_date, 
	vsrc.value AS source,
	vknd.value AS kind 
FROM llogs
JOIN logs.logs l USING (logs_id)
JOIN logs.values vsrc ON vsrc.values_id = source
JOIN logs.values vknd ON vknd.values_id = kind


*/

--JOIN logs.values vval ON vval.attr_type = 25 AND vknd.values_id = ANY(l.values) 


--------- wyswietl wszystkie zródla -----------------
--	SELECT * FROM logs.values2 WHERE attr_type = 20


--------- wyswietl wszystkie rodzaje -----------------
--	SELECT * FROM logs.values2 WHERE attr_type = 7


--------- wyswietl slownik atrybutow -----------------
--	SELECT * FROM logs.d_attr