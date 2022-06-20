-- add new "has_secret" column
alter table PROCESSOR
    add column "has_secret" boolean;

-- update existing processors "has_secret" with 'false' value (the only possible one before this migration)
update PROCESSOR
    set "has_secret"=false;

-- add not null constraint to "has_secret" column
alter table PROCESSOR
    alter column "has_secret" set not null;
