/*
DROP TABLE logs;
DROP  TABLE "values";
DROP TABLE log_values;
DROP TABLE log_data;
DROP TABLE logs.attributes;
*/

--      CREATE LANGUAGE "plpgsql";



CREATE SCHEMA logs
;

CREATE TYPE logs.lattr AS (
        attr_type int,
        data_type int,
        values text[], 
        values_ids int[] 
)
;
----------------- pakiet atrybutow ---------------------


CREATE TYPE logs.lpacket AS (
        uid UUID, 
        expire int, 
        client_date timestamp without time zone, 
        server_date timestamp without time zone, 
        level smallint,
        attributes logs.lattr[]
)
;


CREATE SEQUENCE logs.logs_logs_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1
  CYCLE;
 ;
 
CREATE SEQUENCE logs.values_values_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1
  CYCLE;
 ;
 
CREATE TABLE logs.logs
(
  logs_id integer NOT NULL PRIMARY KEY DEFAULT nextval('logs.logs_logs_id_seq'::regclass),
  server_date timestamp without time zone NOT NULL,
  client_date timestamp without time zone NOT NULL,
  level smallint,
  kind int NOT NULL,
  source int NOT NULL,
  expire integer,
  uid UUID
)
;
CREATE INDEX logs_logs_server_date_idx ON logs.logs (server_date)
;
CREATE TABLE logs.values 
(
  values_id integer NOT NULL PRIMARY KEY DEFAULT nextval('logs.values_values_id_seq'::regclass),
  hash integer,
  value text
)
;
CREATE INDEX idx_values_hash ON logs.values (hash)
;
CREATE TABLE logs.attributes
(
  logs_id integer,	
  attr_type smallint,
  values integer[]
)
;
CREATE INDEX logs_attributes_logs_id_idx ON logs.attributes (logs_id)
;
CREATE INDEX logs_attributes_attr_type_idx ON logs.attributes (attr_type)
;

-- drop table logs.d_attr 



CREATE TABLE logs.d_attr
(
  attr_type smallint NOT NULL PRIMARY KEY,	
  key varchar(3) NOT NULL, 
  name varchar(30) NOT NULL, 
  title varchar(100) NOT NULL,
  required boolean NOT NULL, 
  multiple boolean NOT NULL, 
  max_length integer NOT NULL
)
;
CREATE INDEX logs_d_attr_attr_type_idx ON logs.d_attr (attr_type)
;
CREATE INDEX logs_d_attr_attr_key_idx ON logs.d_attr (key)
;



INSERT INTO logs.d_attr (attr_type, key, name, title, required, multiple, max_length) 
VALUES
	(1, 'lgr', 'logger', 'Logger', false, false, 100),
	(2, 'cls', 'class', 'Klasa', false, false, 300),
	(3, 'mth', 'method', 'Metoda', false, false, 200),
	(4, 'lvl', 'level', 'Poziom', false, false, 10),
	(5, 'lvn', 'level_name', 'Nawa poziomu', false, false, 30),
	(6, 'uid', 'uid', 'UID', false, false, 10000),
	(7, 'knd', 'kind', 'Rodzaj', true, false, 30),
	(8, 'dte', 'date', 'Data', true, false, 23),
	(20, 'src', 'source', 'Źródło', true, false, 100),
	(21, 'adr', 'address', 'Adres', false, true, 100),
	(22, 'dev', 'device', 'Urządzenie / UA', false, false, 200),
	(23, 'usr', 'user', 'Użytkownik', false, false, 100),
	(24, 'tag', 'tag', 'Tag', false, true, 100),
	(25, 'val', 'value', 'Wartość', false, false, 30000),
	(26, 'det', 'details', 'Szczegóły', false, false, 30000),
	(27, 'com', 'comment', 'Komentarz', false, false, 200),
	(30, 'ist', 'instance', 'Instancja', false, false, 50),
	(31, 'ses', 'session', 'Sesja', false, false, 50),
	(32, 'req', 'request', 'Żądanie', false, false, 50),
	(33, 'ver', 'version', 'Wersja', false, false, 20),
	(40, 'prc', 'process_id', 'ID procesu', false, false, 10000),
	(41, 'thr', 'thread_id', 'ID wątku', false, false, 10000),
	(42, 'thn', 'thread_name', 'Nazwa wątku', false, false, 100),
	(43, 'thp', 'thread_priority', 'Priorytet wątku', false, false, 20),
	(44, 'ths', 'thread_state', 'Stan wątku', false, false, 20),
	(50, 'fcl', 'color', 'Kolor czcionki', false, false, 20),
	(51, 'bcl', 'background', 'Kolor tła', false, false, 20),
	(52, 'cex', 'expire_console', 'Czas wygasania w konsoli', false, false, 10),
	(53, 'dex', 'expire_database', 'Czas wygasania w bazie danych', false, false, 10),
	(54, 'url', 'url', 'URL', false, true, 200),
	(60, 'cst', 'call_stack', 'Stos metod', false, true, 300),
	(61, 'est', 'error_stack', 'Stos błędów', false, true, 300),
	(62, 'atr', 'attributes', 'Atrybuty', false, true, 1000),
	(63, 'dta', 'data', 'Dane', false, true, 30000),
	(70, 'flg', 'flags', 'Flagi', false, true, 1000),
	(71, 'prp', 'properties', 'Właściwości', false, true, 100)



;


CREATE OR REPLACE FUNCTION logs.get_hash(value text) RETURNS int AS $$
DECLARE
    result int;
BEGIN
    EXECUTE 'SELECT x''' || substring(md5(value), 1, 8)|| '''::int' INTO result;
    RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE STRICT;


--DROP FUNCTION getValue(text)





CREATE OR REPLACE FUNCTION logs.get_values_id(p_value text) 
RETURNS integer AS $$
DECLARE 
        id integer;
        hh integer;
        rec RECORD;
BEGIN    
        IF p_value IS NULL THEN 
                return null;
        END IF;

        RAISE INFO 'Pobieram wartość indexu "%"', p_value;

        hh = logs.get_hash(p_value);

        FOR rec IN SELECT * FROM logs.values WHERE hash =  hh
        LOOP    
            IF rec.value = p_value THEN
                RAISE INFO 'Zwracam index wartości "%": %', p_value, rec.values_id;
                RETURN rec.values_id;          
            END IF;  
        END LOOP; 

  
        id := (SELECT nextval('logs.values_values_id_seq'::regclass));           
        INSERT INTO logs.values (values_id, hash, value) VALUES (id, hh, p_value);

        RAISE INFO 'Dodaję do słowniks "%": %', p_value, id;
        
        RETURN id;
END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION logs.add(p_packets logs.lpacket[]) 
       RETURNS TABLE (val text, values_id integer) AS $$ 
DECLARE 
    val text;
    attr logs.lattr;
    values_id int;
    values_ids int[];
    lid int;
    pck logs.lpacket;
    fkind int;
    fsource int;
BEGIN  
        fsource := -1;
        fkind := -1;

        --  FOREACH pck IN ARRAY p_packets
        FOR i IN array_lower(p_packets, 1) .. array_upper(p_packets, 1)      
        LOOP
                pck = p_packets[i];
                -- wygeneruj nowy identyfikator
                lid := (SELECT nextval('logs.logs_logs_id_seq'::regclass)); 

               -- FOREACH attr IN ARRAY pck.attributes
                FOR j IN array_lower(pck.attributes, 1) .. array_upper(pck.attributes, 1)
                LOOP 
                        attr = pck.attributes[j];
                        values_ids := array[]::int[];

                        IF array_length(attr.values, 1) <> array_length(attr.values_ids, 1) THEN
                                RAISE EXCEPTION 'Rozmiary tablic wartości i identyfikatorów są różne: % <> %', attr.values, attr.values_ids;
                        END IF;

                        IF attr.data_type > 0 THEN
                                values_ids := array_append(values_ids, attr.data_type * -1);
                        END IF;

                        FOR k IN array_lower(attr.values, 1) .. array_upper(attr.values, 1)
                        LOOP 
                                val := attr.values[k];
                                values_id := attr.values_ids[k];

                                IF (val IS NULL) AND (values_id IS NULL) THEN
                                        RAISE EXCEPTION 'Wartość oraz index są NULL-ami: %', attr;
                                END IF;

                                IF values_id IS NULL THEN                        
                                        values_id := logs.get_values_id(val);
                                END IF;        

                                IF attr.attr_type = 7 THEN fkind := values_id;
                                ELSIF attr.attr_type = 20 THEN fsource := values_id;
                                ELSE values_ids := array_append(values_ids, values_id);
                                END IF;
                                
                                IF NOT val IS NULL THEN 
                                        RETURN QUERY SELECT val, values_id;
                                END IF;
                        END LOOP; 

                        INSERT INTO logs.attributes (logs_id, attr_type, values) 
                        VALUES(lid, attr.attr_type, values_ids);
                END LOOP;

                IF fkind= -1  THEN
                        RAISE EXCEPTION 'Brak atrybutu "kind"';
                END IF;
                
                IF fsource = -1  THEN
                        RAISE EXCEPTION 'Brak atrybutu "source"';
                END IF;
                
                -- dodaj log
                INSERT INTO logs.logs(logs_id, server_date, client_date, expire, uid, level, kind, source )
                VALUES (lid, pck.server_date, pck.client_date, pck.expire, pck.uid, pck.level, fkind, fsource);

        END LOOP; --FOREACH l_packet IN ARRAY p_packets

END; 
$$ LANGUAGE plpgsql; 


---------------------------------------------------------------------------
/*
DROP FUNCTION logs.add(p_packets logs.lpacket[]) ;
DROP TYPE logs.lpacket ;

CREATE TYPE logs.lpacket AS (
        uid UUID, 
        expire int, 
        client_date timestamp without time zone, 
        server_date timestamp without time zone, 
        level int,
        attributes logs.lattr[]
)
;

ALTER TABLE logs.logs ADD COLUMN level smallint
ALTER TABLE logs.logs ADD COLUMN kind int
ALTER TABLE logs.logs ADD COLUMN source  int

*/