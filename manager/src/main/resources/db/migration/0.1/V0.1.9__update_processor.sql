-- add new "type" column
alter table PROCESSOR
    add column "type" varchar(255);

-- update existing processors "type" with 'SINK' value (the only possible one before this migration)
update PROCESSOR
    set "type"='SINK';

-- add not null constraint to "type" column
alter table PROCESSOR
    alter column "type" set not null;
