-- add new "owner" column
alter table BRIDGE
    add column "owner" varchar(255) NOT NULL;

alter table PROCESSOR
    add column "owner" varchar(255) NOT NULL;