/*
1;"address"
2;"source"
3;"tag"
4;"value"
5;"request_id"
6;"session_id"
7;"username"
8;"flags"
9;"call_stack_trace"
10;"error_stack_trace"
*/

select add_log('2012-01-01 12:12:12', '127.0.0.1', 'event', 'TestQRY', 'pgAdmin', 'żądanie', 'sesja', 'miłosz', 'TagTag', 
'Wartość', 123, 23843, 24, 'flagi', null, null);


select add_field(1, 31, 'address', '192.168.1.1');


CREATE OR REPLACE FUNCTION add_field(p_logs_id integer, p_field_id integer, p_field_name text, p_field_value text)
  RETURNS void AS
$BODY$
DECLARE
	fnid	int;
	fvid	int;
	
BEGIN
	fnid = p_field_id;
  
	IF (fnid isNull OR fnid < 0) THEN
		fnid = (SELECT fields_name_id FROM fields_names WHERE fields_names_key ILIKE p_field_name);
	END IF;

	IF (fnid isNull) THEN
		RAISE Exception 'Nie znaleziono typu danej';
	END IF;

	fvid = (SELECT fields_value_id FROM fields_values WHERE fields_values_value ILIKE p_field_value);

	if (fvid isNull) THEN
		fvid = nextval('fields_values_fields_value_id_seq'::regclass);
		INSERT INTO fields_values (fields_value_id, fields_name_id, fields_values_value)
		VALUES (fvid, fnid, p_field_value);
	END IF;

	INSERT INTO fields (logs_id, fileds_value_id) 
	VALUES (p_logs_id, fvid);

  
RETURN;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  ;






















CREATE OR REPLACE FUNCTION add_log(ldate timestamp without time zone, laddress text, lkind text, lsource text, ldevice text, lrequest text, 
  lsession text, lusername text, ltag text, lvalue text, lprocess integer, lthread integer, lexpire integer, lflags text, 
  details_names text[], details_values text[])
  RETURNS void AS
$BODY$
DECLARE
	lsource_id	int;
	laddress_id	int;
	lkind_id	int;
	ldevice_id	int;
	lrequest_id	int;
	lsession_id	int;
	lusername_id	int;
	ltag_id		int;
	
BEGIN
  
	IF (NOT laddress isNull and laddress <> '') THEN
		laddress_id = (SELECT fields_value_id FROM fields_values WHERE fields_name_id = ILIKE laddress);
		IF (laddress_id isNull) THEN
		laddress_id = nextval('addresses_address_id_seq'::regclass);
		INSERT INTO addresses (address_id, address) VALUES (laddress_id, laddress);
		END IF;
	END IF;



	/*IF (NOT lsource isNull and lsource <> '') THEN
		lsource_id = (SELECT source_id FROM sources WHERE source ILIKE lsource);
		IF (lsource_id isNull) THEN
			lsource_id = nextval('sources_source_id_seq'::regclass);
			INSERT INTO sources (source_id, source) VALUES (lsource_id, lsource);
		END IF;
	END IF;

	IF (NOT laddress isNull and laddress <> '') THEN
		laddress_id = (SELECT address_id FROM addresses WHERE address ILIKE laddress);
		IF (laddress_id isNull) THEN
			laddress_id = nextval('addresses_address_id_seq'::regclass);
			INSERT INTO addresses (address_id, address) VALUES (laddress_id, laddress);
		END IF;
	END IF;

	IF (NOT lkind isNull and lkind <> '') THEN
		lkind_id = (SELECT kind_id FROM kinds WHERE kind ILIKE lkind);
		IF (lkind_id isNull) THEN
			lkind_id = nextval('kinds_kind_id_seq'::regclass);
			INSERT INTO kinds (kind_id, kind) VALUES (lkind_id, lkind);
		END IF;
	END IF;

	IF (NOT ldevice isNull and ldevice <> '') THEN
		ldevice_id = (SELECT device_id FROM devices WHERE device ILIKE ldevice);
		IF (ldevice_id isNull) THEN
			ldevice_id = nextval('devices_device_id_seq'::regclass);
			INSERT INTO devices (device_id, device) VALUES (ldevice_id, ldevice);
		END IF;
	END IF;

	IF (NOT lrequest isNull and lrequest <> '') THEN
		lrequest_id = (SELECT request_id FROM requests WHERE request ILIKE lrequest);
		IF (lrequest_id isNull) THEN
			lrequest_id = nextval('requests_request_id_seq'::regclass);
			INSERT INTO requests (request_id, request) VALUES (lrequest_id, lrequest);
		END IF;
	END IF;

	IF (NOT lsession isNull and lsession <> '') THEN
		lsession_id = (SELECT session_id FROM sessions WHERE session ILIKE lsession);
		IF (lsession_id isNull) THEN
			lsession_id = nextval('sessions_session_id_seq'::regclass);
			INSERT INTO sessions (session_id, session) VALUES (lsession_id, lsession);
		END IF;
	END IF;

	IF (NOT lusername isNull and lusername <> '') THEN
		lusername_id = (SELECT username_id FROM usernames WHERE username ILIKE lusername);
		IF (lusername_id isNull) THEN
			lusername_id = nextval('usernames_username_id_seq'::regclass);
			INSERT INTO usernames (username_id, username) VALUES (lusername_id, lusername);
		END IF;
	END IF;

	IF (NOT ltag isNull and ltag <> '') THEN
		ltag_id = (SELECT tag_id FROM tags WHERE tag ILIKE ltag);
		IF (ltag_id isNull) THEN
			ltag_id = nextval('tags_tag_id_seq'::regclass);
			INSERT INTO tags (tag_id, tag) VALUES (ltag_id, ltag);
		END IF;
	END IF;
	*/
	/*****************************************************************************************************/
/*
	INSERT INTO logs (date_db, address_id, source_id, kind_id, tag_id, device_id, session_id, 
		request_id, username_id, thread, process, value, details, flags, expire)
	VALUES (ldate, laddress_id, lsource_id, lkind_id, ltag_id, ldevice_id, lsession_id, 
		lrequest_id, lusername_id, lthread, lprocess, lvalue, ldetails, lflags, lexpire);
*/

  
RETURN;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE