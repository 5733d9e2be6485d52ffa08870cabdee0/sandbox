alter table BRIDGE
    add column "subscription_id" varchar(255);

update BRIDGE set subscription_id=gen_random_uuid();

alter table BRIDGE
    alter column subscription_id set not null;