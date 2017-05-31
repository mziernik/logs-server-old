CREATE TABLE logs
(
  logs_id serial NOT NULL,
  date timestamp without time zone DEFAULT now(),
  date_db timestamp without time zone NOT NULL DEFAULT now(),
  process integer,
  thread integer,  
  expire integer,
  CONSTRAINT logs_pkey PRIMARY KEY (logs_id)
)
;
CREATE TABLE storage
(
  id serial NOT NULL,
  date timestamp without time zone NOT NULL DEFAULT now(),
  path text,
  source text,
  name text,
  size integer,
  expire integer,
  CONSTRAINT storage_pkey PRIMARY KEY (id)
)
;
CREATE TABLE users
(
  users_id serial NOT NULL,
  username text NOT NULL,
  password text, -- MD5
  superuser boolean NOT NULL DEFAULT false,
  config text,
  CONSTRAINT users_pkey PRIMARY KEY (users_id)
)
;
CREATE TABLE field_names
(
  field_name_id integer NOT NULL,
  field_key text NOT NULL,
  field_caption text,
  field_internal boolean NOT NULL,
  CONSTRAINT field_names_pkey PRIMARY KEY (field_name_id),
  CONSTRAINT field_name_field_name_id_key UNIQUE (field_name_id),
  CONSTRAINT field_name_field_name_key_key UNIQUE (field_key)
)
;
CREATE TABLE field_values
(
  field_value_id serial NOT NULL,
  field_value text,
  CONSTRAINT field_values_pkey PRIMARY KEY (field_value_id)
)
;
CREATE TABLE fields
(
  field_id serial NOT NULL,
  logs_id integer NOT NULL,
  field_name_id integer NOT NULL,
  field_value_id integer NOT NULL,
  CONSTRAINT fields_pkey PRIMARY KEY (field_id),
  CONSTRAINT fields_field_name_id_fkey FOREIGN KEY (field_name_id)
      REFERENCES field_names (field_name_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fields_field_value_id_fkey FOREIGN KEY (field_value_id)
      REFERENCES field_values (field_value_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fields_logs_id_fkey FOREIGN KEY (logs_id)
      REFERENCES logs (logs_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
;
/*********************************************************************************************************************************************/
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (0, 'kind', 'Rodzaj logu', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (1, 'address', 'Adres, z którego wysłany został log', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (2, 'source', 'Nazwa aplikacji/usługi źródłowej', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (3, 'device', 'Nazwa urządzenia lub User Agent', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (4, 'tag', 'Tag, ułatwiający grupowanie i filtrowanie logów', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (5, 'value', 'Wartość logu', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (6, 'details', 'Szczegóły logu', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (7, 'request_id', 'Identyfikator żądania lub pojedynczej operacji', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (8, 'session_id', 'Identyfikator sesji lub instancji aplikacji', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (9, 'username', 'Nazwa użytkownika', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (10, 'flags', 'Dodatkowe flagi', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (11, 'call_stack_trace', 'Stos wywołań metod', true)
;
INSERT INTO field_names(field_name_id, field_key, field_caption, field_internal) 
VALUES (12, 'error_stack_trace', 'Stos wywołań błędów', true)
;
