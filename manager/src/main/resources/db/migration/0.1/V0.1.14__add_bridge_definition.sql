-- add new "definition" column
alter table BRIDGE
    add column "definition" jsonb NOT NULL;
