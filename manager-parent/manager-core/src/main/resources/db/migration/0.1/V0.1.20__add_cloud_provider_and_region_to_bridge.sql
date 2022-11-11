alter table BRIDGE
    add column "cloud_provider" varchar(255),
    add column "region"         varchar(255);
update BRIDGE
set cloud_provider='aws',
    region='us-east-1';
alter table BRIDGE
    alter column cloud_provider set not null,
    alter column region set not null;