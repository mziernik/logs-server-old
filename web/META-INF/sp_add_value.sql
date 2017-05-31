-- Function: add_value(text)

-- DROP FUNCTION add_value(text);

CREATE OR REPLACE FUNCTION add_value(p_value text)
  RETURNS integer AS
$BODY$
DECLARE
	id	int;

BEGIN	
	IF (p_value isNull) THEN
		RETURN NULL;
	END IF;

	id = (SELECT values_id FROM values WHERE value = p_value);

	IF (id isNull) THEN
	 	id = nextval('values_values_id_seq'::regclass);
		INSERT INTO values (values_id, value) VALUES (id, p_value);
	END IF;
	
	RETURN id;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION add_value(text)
  OWNER TO db;
