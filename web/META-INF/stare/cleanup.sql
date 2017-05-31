CREATE OR REPLACE FUNCTION cleanup()
  RETURNS integer AS
$BODY$
BEGIN
	DELETE FROM logs 
	WHERE (NOT date_db isNull) 
		AND (expire > 0) 
		AND (now() - date_db > expire * '1 hour'::interval);

	DELETE FROM sources WHERE NOT source_id IN (SELECT source_id FROM logs);
	DELETE FROM kinds WHERE NOT kind_id IN (SELECT kind_id FROM logs);
	DELETE FROM addresses WHERE NOT address_id IN (SELECT address_id FROM logs);
	DELETE FROM devices WHERE NOT device_id IN (SELECT device_id FROM logs);
	DELETE FROM tags WHERE NOT tag_id IN (SELECT tag_id FROM logs);
	DELETE FROM usernames WHERE NOT username_id IN (SELECT username_id FROM logs);
	DELETE FROM requests WHERE NOT request_id IN (SELECT request_id FROM logs);
	DELETE FROM sessions WHERE NOT session_id IN (SELECT session_id FROM logs);

	DELETE FROM storage 
	WHERE (NOT date isNull) 
		AND (expire > 0) 
		AND (now() - date > expire * '1 hour'::interval);

	RETURN 0;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE