-- add new "organisation_id" column
alter table BRIDGE
    add column "organisation_id" varchar(255) NOT NULL;