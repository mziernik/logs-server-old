-- Function: logs.find_values_arr(text[])

-- DROP FUNCTION logs.find_values_arr(text[]);

CREATE OR REPLACE FUNCTION logs.find_values_arr(p_values text[])
  RETURNS integer[] AS
$BODY$
DECLARE 
        id integer;
        hh integer;
        rec RECORD;
        hashes int[];
	vals int[];
	result int[];
BEGIN    
        IF p_values IS NULL THEN 
                return null;
        END IF;

	hashes := array[]::int[];
	result := array[]::int[];
	--hashes := array[array_length(p_values)]::int[];

	
	FOR i IN array_lower(p_values, 1) .. array_upper(p_values, 1)      
        LOOP
		hashes := array_append(hashes, logs.get_hash(p_values[i]));
	END LOOP;

	
	vals := array[]::text[];

        FOR rec IN SELECT * FROM logs.values WHERE hash = ANY(hashes)
        LOOP        
            IF rec.value = any(p_values) THEN
		result := array_append(result, rec.values_id);
             END IF;  
        END LOOP; 
	return result;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION logs.find_values_arr(text[])
  OWNER TO db;
