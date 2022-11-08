-- add new "generation" column
alter table BRIDGE
    add column "generation" integer NOT NULL default 0;

alter table PROCESSOR
    add column "generation" integer NOT NULL default 0;

alter table CONNECTOR
    add column "generation" integer NOT NULL default 0;
