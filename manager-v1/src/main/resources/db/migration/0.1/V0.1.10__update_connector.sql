-- rename "connector_type" to "connector_type_id"
alter table CONNECTOR
    rename column connector_type to connector_type_id;

-- add new "type" column
alter table CONNECTOR
    add column "type" varchar(255);

-- update existing connectors "type" with 'SINK' value (the only possible one before this migration)
update CONNECTOR
    set "type"='SINK';

-- add not null constraint to "type" column
alter table CONNECTOR
    alter column "type" set not null;
