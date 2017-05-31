/*
1;"kind"
2;"address"
3;"source"
4;"device"
5;"username"
6;"tag"
7;"value"
8;"details"
9;"request_id"
10;"session_id"
11;"flags"
12;"call_stack_trace"
13;"error_stack_trace"
*/

--select add_log('2012-01-01 12:12:12', '127.0.0.1', 'event', 'TestQRY', 'pgAdmin', 'żądanie', 'sesja', 'miłosz', 'TagTag', 
--'Wartość', 'det', 123, 23843, 24, 'flagi', null, null);


/*
  Dodaje log wraz z wartościami pól, zwraca identyfikator logu
*/

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
	p_details text, 
	p_process integer, 
	p_thread integer, 
	p_expire integer, 
	p_flags text, 
	p_call_stack_trace text,
	p_error_stack_trace text)
  RETURNS integer AS
$BODY$
DECLARE
	lid	int;
BEGIN
	lid = nextval('logs_logs_id_seq'::regclass);

	INSERT INTO logs (logs_id, date, process, thread, expire)
	VALUES (lid, p_date, p_process, p_thread, p_expire);

	PERFORM  add_field(lid, 1, null, p_kind);
	PERFORM  add_field(lid, 2, null, p_address);
	PERFORM  add_field(lid, 3, null, p_source);
	PERFORM  add_field(lid, 4, null, p_device);
	PERFORM  add_field(lid, 5, null, p_username);
	PERFORM  add_field(lid, 6, null, p_tag);
	PERFORM  add_field(lid, 7, null, p_value);
	PERFORM  add_field(lid, 8, null, p_details);
	PERFORM  add_field(lid, 9, null, p_request_id);
	PERFORM  add_field(lid, 10, null, p_session_id);	
	PERFORM  add_field(lid, 11, null, p_flags);
	PERFORM  add_field(lid, 12, null, p_call_stack_trace);
	PERFORM  add_field(lid, 13, null, p_error_stack_trace);

  
RETURN lid;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE