/*
select logs.add('e8804370-0093-11e4-9191-0800200c9a66'::UUID, 12, '2014-01-01 12:12:22.3434',
   ARRAY[ 
        ( 20, 0, ARRAY['nazwa źródła'], ARRAY[null, null, null]::int[]),
        ( 13, 2, ARRAY['a', '2'], ARRAY[null]::int[])

]::logs.lrec[]);

*/


------------------------------ WYŚWIETL WSZYSTKIE ŹRÓDŁA ----------------------------------------

/*
SELECT count(1), v.values_id, v.hash, v.value
FROM logs.values v
JOIN logs.attributes a ON v.values_id = a.values[1] AND a.attr_type = 20  
GROUP BY values_id, hash, value


;
*/
/*
WITH Q AS (
        SELECT * FROM logs.logs 
        ORDER BY server_date DESC
        LIMIT 10
)
SELECT * FROM Q

JOIN logs.attributes a ON (Q.logs_id = a.logs_id  )
AND (
        (attr_type = 20) OR (attr_type = 24) OR (attr_type = 25)
)

*/




---------------- WYCZYSC BAZE -----------------------
/*
DELETE FROM logs.values;
DELETE FROM logs.attributes;
DELETE FROM logs.logs;
*/
------------------------------------------------------



------- przepisanie źródła z attributes do logs na podstawie logs_id  
UPDATE logs.logs SET source = T.values_id FROM(
        SELECT logs_id, v.values_id
        FROM logs.values v
        JOIN logs.attributes a ON v.values_id = a.values[1] AND a.attr_type = 20  
) T  
WHERE logs.logs.LOGS_ID = T.LOGS_ID



------- przepisanie rodzaju z attributes do logs na podstawie logs_id  
UPDATE logs.logs SET kind = T.values_id FROM(
        SELECT logs_id, v.values_id
        FROM logs.values v
        JOIN logs.attributes a ON v.values_id = a.values[1] AND a.attr_type = 7  
) T  
WHERE logs.logs.LOGS_ID = T.LOGS_ID





SELECT * FROM logs.add(
ARRAY[
(
        '8728f4f2-3f11-4b8e-b49a-81ebff45ab90',
        null,
        '2014-07-11 09:29:59.808',
        '2014-07-11 09:29:51.111',
        10,        
        ARRAY[
                (20, 0, ARRAY['Squid Milosz'],ARRAY[null]),
                (7, 0, ARRAY['event'],ARRAY[null]),
                (22, 0, ARRAY['DropboxDesktopClient/2.8.2 (Windows; 7; i32; pl)'],ARRAY[null]),
                (21, 0, ARRAY['127.0.0.1', '10.1.0.254', '', ''],ARRAY[null,null,null,null]),
                (24, 0, ARRAY['GET, 200'],ARRAY[null]),
                (27, 0, ARRAY['910 B, 55 719 ms, text/plain, 10.1.0.16'],ARRAY[null]),
                (1, 0, ARRAY['Squid'],ARRAY[null]),
                (25, 0, ARRAY['','','http://notify2.dropbox.com/subscribe'],ARRAY[null,null,null])
        ]::logs.lattr[]
),
(
        '974358a0-7fd0-4901-8e40-23329ca5ef76',
        null,
        '2014-07-11 09:42:17.161',
        '2014-07-11 09:42:11.543',
        53,
        ARRAY[
                (20, 0, ARRAY['Logi'],ARRAY[62]),
                (7, 0, ARRAY['request'],ARRAY[179]),
                (22, 0, ARRAY['Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36 OPR/22.0.1471.40 (Edition Next)'],ARRAY[24]),
                (23, 0, ARRAY['milosz.ziernik'],ARRAY[180]),
                (21, 0, ARRAY['10.1.0.254'],ARRAY[17]),
                (24, 0, ARRAY['Request'],ARRAY[181]),
                (60, 0, ARRAY['com.logs.ServletLog.requestInfo (ServletLog.java:311)','com.servlet.handlers.BPage.<init> (BPage.java:237)','service.handlers.Page.<init> (Page.java:18)','pages.PConsole.<init> (PConsole.java:26)','sun.reflect.NativeConstructorAccessorImpl.newInstance0','sun.reflect.NativeConstructorAccessorImpl.newInstance','sun.reflect.DelegatingConstructorAccessorImpl.newInstance','java.lang.reflect.Constructor.newInstance','java.lang.Class.newInstance','com.servlet.handlers.BPage.newInstance (BPage.java:134)','com.servlet.MainServlet.handleRequest (MainServlet.java:237)','com.servlet.MainServlet.processRequest (MainServlet.java:90)','com.servlet.MainServlet$1.run (MainServlet.java:62)','java.util.concurrent.ThreadPoolExecutor.runWorker','java.util.concurrent.ThreadPoolExecutor$Worker.run','java.lang.Thread.run'],ARRAY[182,183,184,null,186,187,188,189,190,191,192,193,194,195,196,197]),
                (2, 0, ARRAY['com.logs.ServletLog'],ARRAY[198]),
                (3, 0, ARRAY['requestInfo (ServletLog.java:311)'],ARRAY[199]),
                (1, 0, ARRAY['MLogger default v1.69 via udp://10.1.0.254:5140'],ARRAY[70]),
                (40, 0, ARRAY['9768'],ARRAY[71]),
                (31, 0, ARRAY['121'],ARRAY[null]),
                (42, 0, ARRAY['Request, GET, /'],ARRAY[null]),
                (43, 0, ARRAY['5'],ARRAY[74]),
                (32, 0, ARRAY['56b95e7baa664b8ba36ba26f3b5035f6'],ARRAY[null]),
                (31, 0, ARRAY['334B2681F4DAA8780B1FBF4C1612E557'],ARRAY[203]),
                (54, 0, ARRAY['http://milosz/logi/'],ARRAY[null]),
                (33, 0, ARRAY['1.5.1533'],ARRAY[75]),
                (25, 0, ARRAY['','GET, http://milosz/logi/'],ARRAY[21,null])
        ]::logs.lattr[]
)
]::logs.lpacket[]
) ORDER BY val, values_id



