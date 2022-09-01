-- add new error columns
alter table BRIDGE
    add column "error_id" integer,
    add column "error_uuid" varchar(255);

alter table PROCESSOR
    add column "error_id" integer,
    add column "error_uuid" varchar(255);

alter table CONNECTOR
    add column "error_id" integer,
    add column "error_uuid" varchar(255);
