-- Function: display_filters(text[], text[])

-- DROP FUNCTION display_filters(text[], text[]);

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
	p_phrases_exclude text[]
	)
  RETURNS integer[] AS
$BODY$
DECLARE
	result	integer[];
BEGIN
	result = array(SELECT DISTINCT logs_id FROM logs);
	
	IF NOT p_kinds IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id = 1 
			AND field_value = ANY(p_kinds));
	END IF;

	IF NOT p_addresses IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result) 
			AND field_name_id = 2 
			AND field_value = ANY(p_addresses));
	END IF;

	IF NOT p_sources IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id = 3 
			AND field_value = ANY(p_sources));
	END IF;

	IF NOT p_devices IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id = 4 
			AND field_value = ANY(p_devices));	
	END IF;

	IF NOT p_usernames IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id = 5 
			AND field_value = ANY(p_usernames));	
	END IF;

	IF NOT p_tags IsNull THEN
		result = array(SELECT DISTINCT logs_id FROM fields 
			JOIN field_values USING (field_value_id) 
			WHERE logs_id = ANY(result)
			AND field_name_id = 6 
			AND field_value = ANY(p_tags));	
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
  LANGUAGE plpgsql VOLATILE
;


select distinct logs_id, date, field_name_id, field_key, field_value from fields
join field_names using (field_name_id)
join field_values using (field_value_id)
join logs using (logs_id)
where logs_id = ANY (

	filter_logs(array['error', 'event'], array['localhost', '127.0.0.1'], array['Logs', 'ContentServer'], null, array['admin'], 
		array['InternalError', 'aa'], null, null, null, array['%B£¥D%'], array['phr e'])

)

order by logs_id



 

--select display_filters(array['error', 'event'], array['localhost'], array['Logs'], null, array['admin'], array['InternalError', 'aa'],
 --NULL, null, 'req', 'phr i', 'phr e');

/*
	p_kinds text[], 
	p_addresses text[], 
	p_sources text[],
	p_devices text[],
	p_usernames text[],
	p_tags text[],
	p_date_from timestamp without time zone,
	p_date_to timestamp without time zone,
	p_request text,
	p_phrase_include text,
	p_phrase_exclude text
	*/