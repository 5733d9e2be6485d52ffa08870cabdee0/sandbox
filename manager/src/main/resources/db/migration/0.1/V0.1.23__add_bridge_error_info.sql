-- add new "error_code" column
alter table BRIDGE
    add column "bridge_error_id" integer,
    add column "bridge_error_uuid" varchar(255);

alter table PROCESSOR
    add column "bridge_error_id" integer,
    add column "bridge_error_uuid" varchar(255);
