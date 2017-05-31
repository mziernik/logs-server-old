-- Function: filter_logs(text[], text[], text[], text[], text[], text[], timestamp without time zone, timestamp without time zone, text[], text[], text[])

-- DROP FUNCTION filter_logs(text[], text[], text[], text[], text[], text[], timestamp without time zone, timestamp without time zone, text[], text[], text[]);

CREATE OR REPLACE FUNCTION filter_logs(
	p_kinds text[], 
	p_addresses text[], 
	p_sources text[], 
	p_devices text[], 
	p_usernames text[], 
	p_tags text[], 
	p_date_from timestamp without time zone, 
	p_date_to timestamp without time zone, 
	p_requests text[], 
	p_phrases_include text[], 
	p_phrases_exclude text[])
  RETURNS integer[] AS
$BODY$
DECLARE
	result	integer[];
BEGIN
	result = array(SELECT DISTINCT logs_id FROM logs ORDER BY logs_id);
	
	IF NOT p_kinds IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_kinds)
			AND kind = values_id 
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);
	END IF;

	

	IF NOT p_addresses IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_addresses)
			AND address= values_id
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);
	END IF;

	IF NOT p_sources IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_sources)
			AND source= values_id
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);
	END IF;

	IF NOT p_devices IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_devices)
			AND device = values_id
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);	
	END IF;

	IF NOT p_usernames IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_usernames)
			AND username = values_id
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);
	END IF;

	IF NOT p_tags IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM logs
			JOIN values ON values.value = ANY(p_tags)
			AND tag = values_id
			WHERE logs_id = ANY(result)
			ORDER BY logs_id);
	END IF;


	IF NOT p_date_from IsNull THEN
		result = array(select logs_id FROM logs
		WHERE logs_id = ANY(result)
		AND date >= p_date_from);
	END IF;

	IF NOT p_date_to IsNull THEN
		result = array(select logs_id FROM logs 
		WHERE logs_id = ANY(result) 
		AND date <= p_date_to);
	END IF;


	IF NOT p_requests IsNull THEN -- zadanie
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id in (9 ,10) 
			AND field_value ILIKE ANY (p_requests));
	END IF;
	

	IF NOT p_phrases_include IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result) 
			AND NOT field_name_id in (1, 2, 3, 4, 5, 6) 
			AND field_value ILIKE ANY (p_phrases_include));
	END IF;

	IF NOT p_phrases_exclude IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result) 
			AND NOT field_name_id in (1, 2, 3, 4, 5, 6) 
			AND NOT field_value ILIKE ANY (p_phrases_exclude));
	END IF;

	RETURN result;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;


select filter_logs(array['error', 'event', 'query'], array['127.0.0.1'], null, null, array['admin'], null, NULL, null, null, null, null);


/*
select logs_id from logs
join values on 
(kind = values_id and values.value = 'query')
AND (address = values_id and values.value = '127.0.0.1')
*/