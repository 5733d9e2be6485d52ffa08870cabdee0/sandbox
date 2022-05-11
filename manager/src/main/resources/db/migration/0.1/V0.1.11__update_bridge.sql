-- add new "organisation_id" column
alter table BRIDGE
    add column "organisation_id" varchar(255);

-- update existing BRIDGE "organisation_id" with '15247674' value (the only possible one before this migration)
update BRIDGE
set "organisation_id"='15247674';

-- add not null constraint to "organisation_id" column
alter table BRIDGE
    alter column "organisation_id" set not null;