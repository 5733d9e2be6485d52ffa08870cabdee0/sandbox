-- add new "expire_at" column
alter table BRIDGE
    add column "expire_at" timestamp;

-- add new "instance_type" column
alter table BRIDGE
    add column "instance_type" varchar(255);

-- update existing bridge instance of type EVAL
update BRIDGE
set "instance_type"='EVAL';

-- add not null constraint to "instance_type" column
alter table BRIDGE
    alter column "instance_type" set not null;