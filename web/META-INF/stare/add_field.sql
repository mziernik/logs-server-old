/*
  Dodaje pojedyncze pole. W pierwszej kolejności brany jest pod uwagę identyfikator pola field_id. 
  Jeśli wartość nie istnieje, identyfikator ustalany jest na podstawie nazwy.
*/

CREATE OR REPLACE FUNCTION add_field(p_logs_id integer, p_field_id integer, p_field_name text, p_field_value text)
  RETURNS void AS
$BODY$
DECLARE
	fnid	int;
	fvid	int;	
BEGIN	
	IF (p_field_value isNull OR p_field_value ILIKE '') THEN
		RETURN;
	END IF;
	
	-- czy istnieje log
	IF ((SELECT (1) FROM logs WHERE logs_id = p_logs_id) IsNull) THEN
		RAISE EXCEPTION 'Nie znaleziono logu o id %', p_logs_id;
	END IF;

	-- jesli odwolanie jest na podstawie id pola, sprawdz czy istnieje
	IF (NOT p_field_id IsNull AND (SELECT (1) FROM field_names WHERE field_name_id = p_field_id) IsNull) THEN
		RAISE EXCEPTION 'Nie znaleziono typu o id %)', p_field_id;
	END IF;

	fnid = p_field_id;

	-- jesli nie podano identyfikator, to ustal na podstawie nazwy
	IF (fnid isNull OR fnid < 0) THEN
		fnid = (SELECT field_name_id FROM field_names WHERE field_key ILIKE p_field_name);
		IF (fnid IsNull) THEN
			RAISE EXCEPTION 'Nie znaleziono typu "%"', p_field_name;
		END IF;
	END IF;

	fvid = (SELECT field_value_id FROM field_values WHERE field_value ILIKE p_field_value);

	if (fvid isNull) THEN
		fvid = nextval('field_values_field_value_id_seq'::regclass);
		INSERT INTO field_values (field_value_id, field_value)
		VALUES (fvid, p_field_value);
	END IF;

	INSERT INTO fields (logs_id, field_name_id, field_value_id) 
	VALUES (p_logs_id, fnid, fvid);
  
RETURN;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE