-- add new "has_secrets" column
alter table PROCESSOR
    add column "has_secrets" boolean;

-- update existing processors "has_secrets" with 'false' value (the only possible one before this migration)
update PROCESSOR
    set "has_secrets"=false;

-- add not null constraint to "has_secrets" column
alter table PROCESSOR
    alter column "has_secrets" set not null;
