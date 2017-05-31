/*

CREATE TYPE logs.lvalue AS (
	id int,
	value text
)

CREATE TYPE logs.filter_group AS (
	   attr_type int,
       operator int,
	   value text
)

CREATE TYPE logs.filter_packet AS (
	 groups logs.filter_group[]	
)


--drop type logs.lattr cascade 

CREATE TYPE logs.lattr AS (
        attr_type int,
        data_type int,
        values logs.lvalue[]
)

--drop type logs.lpacket cascade 

CREATE TYPE logs.lpacket AS (
        uid UUID, 
        expire int, 
        client_date timestamp without time zone, 
        server_date timestamp without time zone, 
        level smallint,
        attributes logs.lattr[]
)
*/

CREATE OR REPLACE FUNCTION logs.get_hash(value text)
  RETURNS integer AS
$BODY$
DECLARE
    result int;
BEGIN
    EXECUTE 'SELECT x''' || substring(md5(value), 1, 8)|| '''::int' INTO result;
    RETURN result;
END;
$BODY$
  LANGUAGE plpgsql;

/********************************************************************************************************************/

/**
		Zwraca identyfikator wartości na podstawie treści
*/
CREATE OR REPLACE FUNCTION logs.find_value(p_attr_type smallint, p_value text) 
RETURNS integer AS $$
DECLARE 
        id integer;
        hh integer;
        rec RECORD;
BEGIN    
        IF p_value IS NULL THEN 
                return null;
        END IF;

  
        hh = logs.get_hash(p_value);

        FOR rec IN SELECT * FROM logs.values WHERE hash = hh AND attr_type = p_attr_type
        LOOP    
            IF rec.value = p_value THEN
                RETURN rec.values_id;          
            END IF;  
        END LOOP; 
        
        RETURN null;
END;
$$ LANGUAGE plpgsql;

/********************************************************************************************************************/

CREATE OR REPLACE FUNCTION logs.get_or_add_value(p_attr_type smallint, p_data_type smallint, p_value text) 
RETURNS integer AS $$
DECLARE 
        id integer;
        rec RECORD;
BEGIN    
        IF p_value IS NULL THEN 
                return null;
        END IF;

        id := logs.find_value(p_attr_type, p_value);
        IF id IsNull THEN 
		id := (SELECT nextval('logs.values_values_id_seq'::regclass));           
		INSERT INTO logs.values (attr_type, data_type, values_id, hash, value) 
		VALUES (p_attr_type, p_data_type, id, logs.get_hash(p_value), p_value);

		RAISE INFO 'Dodaję do słownika %, "%": %', p_attr_type, p_value, id;
	END IF;
        
        RETURN id;
END;
$$ LANGUAGE plpgsql;

/********************************************************************************************************************/

/**
	Zwraca identyfikatory fraz podanych w parametrze, porownanie equals
*/
CREATE OR REPLACE FUNCTION logs.find_values(p_values text[]) 
RETURNS TABLE (values_id integer, attr_type smallint, val text) AS $$
DECLARE 
        id integer;
        hh integer;
        rec RECORD;
        hashes int[];
	vals int[];
BEGIN    
        IF p_values IS NULL THEN 
                return;
        END IF;

	hashes := array[]::int[];
	--hashes := array[array_length(p_values)]::int[];

	
	FOR i IN array_lower(p_values, 1) .. array_upper(p_values, 1)      
        LOOP
		hashes := array_append(hashes, logs.get_hash(p_values[i]));
	END LOOP;

	
	vals := array[]::text[];

        FOR rec IN SELECT * FROM logs.values WHERE hash = ANY(hashes) 
        LOOP        
            IF rec.value = any(p_values) THEN
		    RETURN QUERY SELECT rec.values_id, rec.attr_type, rec.value;       
            END IF;  

        END LOOP; 
END;
$$ LANGUAGE plpgsql;

/********************************************************************************************************************/


CREATE OR REPLACE FUNCTION logs.add(p_packets logs.lpacket[]) 
       RETURNS TABLE (val text, values_id integer, attr_type smallint) AS $$ 
BEGIN  
        --  FOREACH pck IN ARRAY p_packets
        FOR i IN array_lower(p_packets, 1) .. array_upper(p_packets, 1)      
        LOOP

		RETURN QUERY SELECT * FROM logs.add(p_packets[i]);             
        END LOOP; 

END; 
$$ LANGUAGE plpgsql; 

/********************************************************************************************************************/


CREATE OR REPLACE FUNCTION logs.add(pck logs.lpacket) 
       RETURNS TABLE (val text, values_id integer, attr_type smallint) AS $$ 
DECLARE 
    val text;
    attr logs.lattr;
    values_id int;
    values_ids int[];
    lid int;
    fkind int;
    fsource int;
    attrs logs.attr[];
BEGIN 
   -- wygeneruj nowy identyfikator
	lid := (SELECT nextval('logs.logs_logs_id_seq'::regclass)); 

	attrs := array[]::logs.attr[];

	-- FOREACH attr IN ARRAY pck.attributes
	FOR j IN array_lower(pck.attributes, 1) .. array_upper(pck.attributes, 1)
	LOOP 
		attr = pck.attributes[j];
		values_ids := array[]::int[];

		IF attr.data_type > 0 THEN
			values_ids := array_append(values_ids, attr.data_type * -1);
		END IF;

		FOR k IN array_lower(attr.values, 1) .. array_upper(attr.values, 1)
		LOOP 
			val := attr.values[k].value;
			values_id := attr.values[k].id;

			IF (val IS NULL) AND (values_id IS NULL) THEN
				RAISE EXCEPTION 'Wartość oraz index są NULL-ami: %', attr;
			END IF;

			IF values_id IS NULL THEN                        
				values_id := logs.get_or_add_value(attr.attr_type::smallint, attr.data_type::smallint, val);
			END IF;        

			IF attr.attr_type = 7 THEN fkind := values_id;
			ELSIF attr.attr_type = 20 THEN fsource := values_id;
			ELSE values_ids := array_append(values_ids, values_id);
			END IF;
			
			IF NOT val IS NULL THEN 
				RETURN QUERY SELECT val, values_id, attr.attr_type::smallint;
			END IF;
		END LOOP; 
		
		attrs := array_append(attrs, (attr.attr_type, values_ids)::logs.attr );

		INSERT INTO logs.attributes (logs_id, data_type, values) 
		VALUES(lid, attr.data_type, values_ids);
	END LOOP;

	IF fkind= -1  THEN
		RAISE EXCEPTION 'Brak atrybutu "kind"';
	END IF;

	IF fsource = -1  THEN
		RAISE EXCEPTION 'Brak atrybutu "source"';
	END IF;

			--all_vals := array(SELECT unnest(all_vals) AS all_vals ORDER BY all_vals);
	-- dodaj log
	INSERT INTO logs.logs(logs_id, server_date, client_date, expire, uid, level, kind, source, attributes)
	VALUES (lid, pck.server_date, pck.client_date, pck.expire, pck.uid, pck.level, fkind, fsource, attrs);

END; 
$$ LANGUAGE plpgsql; 

/*******************************************************************************************************/

/*  testy */

/*
SELECT 
	logs.get_hash('abc') as hash,
	logs.get_or_add_value(1::smallint, 25::smallint, 'abc');

*/

--	SELECT * from logs.find_values(ARRAY['user', 'abc', 'tomcat']);



-- DELETE FROM logs.values WHERE values_id IN (SELECT values_id from logs.find_values(ARRAY['abc', 'tomcat']));




--select * from logs.values where values_id = 2125366