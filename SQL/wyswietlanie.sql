
---------------------------- URL <> 'aaaa' ----------------------------
/*
WITH l as (
        SELECT l.logs_id, a.attr_type, a.name 
        FROM logs.logs l, logs.d_attr a
        WHERE L.logs_id < 300
)

SELECT l.logs_id, l.attr_type, l.name, v2.value FROM L

LEFT JOIN logs.attributes la ON la.logs_id = l.logs_id and l.attr_type = la.attr_type
LEFT JOIN logs.values v2 ON (v2.values_id = ANY(LA.values))

WHERE l.attr_type = 54 AND (value isNull OR value <> 'aaaa')
ORDER BY l.logs_id, l.attr_type, la.attr_type

LIMIT 100
*/





------------------------------ szczegoly logu ---------------------------------

SELECT l.logs_id , a2.attr_type ,da1.name,  v2.value 
FROM logs.logs l
LEFT JOIN logs.attributes a2 ON a2.logs_id = l.logs_id 
LEFT JOIN logs.values v2 ON (v2.values_id = ANY(a2.values))
LEFT JOIN logs.d_attr da1 ON a2.attr_type = da1.attr_type
WHERE l.logs_id = 13











/*
------------------------------------ test -------------------------------

SELECT l.logs_id, a2.attr_type, v2.value
FROM logs.logs l

LEFT JOIN logs.attributes a2 ON a2.logs_id = l.logs_id 
LEFT JOIN logs.values v2 ON (v2.values_id = ANY(a2.values))

WHERE l.logs_id = 13
order by logs_id, attr_type

*/