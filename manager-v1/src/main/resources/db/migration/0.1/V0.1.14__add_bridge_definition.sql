-- add new "definition" column
alter table BRIDGE
    add column "definition" jsonb;

-- update existing bridges "definition" with '{}' value
update BRIDGE
    set "definition" = '{}'::json;

-- add not null constraint to "definition" column
alter table BRIDGE
    alter column "definition" set not null;
