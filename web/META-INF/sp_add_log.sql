CREATE OR REPLACE FUNCTION add_log(
	p_date timestamp without time zone, 
	p_address text, 
	p_kind text,
	p_source text, 
	p_device text, 
	p_request_id text, 
	p_session_id text, 
	p_username text, 
	p_tag text, 
	p_value text, 
	p_process integer, 
	p_thread integer, 
	p_expire integer, 
	p_other_names text[],
	p_other_values text[])
	
  RETURNS integer AS
$BODY$
DECLARE
	id	int;
	nid	int;
	vid	int;
	i	int;

BEGIN
	id = nextval('logs_logs_id_seq'::regclass);

	INSERT INTO logs (logs_id, date, process, thread, expire, kind, address, source, device, username, tag, value, session, request)
	VALUES (id,
		p_date, 
		p_process, 
		p_thread, 
		p_expire,
		add_value(p_kind),
		add_value(p_address),
		add_value(p_source),
		add_value(p_device),
		add_value(p_username),
		add_value(p_tag),
		add_value(p_value),
		add_value(p_session_id),
		add_value(p_request_id)
	);

	IF (NOT p_other_names IsNull 
		AND NOT p_other_values IsNull 
		AND array_upper(p_other_names,1) = array_upper(p_other_values,1)) THEN
	
		FOR i IN array_lower(p_other_names,1)..array_upper(p_other_names,1)
		LOOP
			-- RAISE NOTICE 'LOOP: %: %',  p_other_names[i], p_other_values[i];  
		
			nid = (SELECT names_id FROM names WHERE key = p_other_names[i]);

			IF (nid IsNull) THEN
				nid = nextval('names_names_id_seq'::regclass);
				INSERT INTO names (names_id, key, internal) VALUES (nid, p_other_names[i], false);
			END IF;

			vid = add_value( p_other_values[i]);		
			INSERT INTO others (logs_id, names_id, values_id) VALUES (id, nid,  vid);	
		
		END LOOP;

	END IF;
  
	RETURN id;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE

;

select add_log('2012-01-01 12:12:12', '127.0.0.1', 'event', 'TestQRY', 'pgAdmin', '¿¹danie', 'sesja', 'mi³osz', 'TagTag', 
'Wartoœæ', 'det', 123, 23843, 24, ARRAY['FILED', 'CALL-STACK'], ARRAY['POLE', 'eeeeeeeeeeeeeeeeeeeeeeeeee']);
