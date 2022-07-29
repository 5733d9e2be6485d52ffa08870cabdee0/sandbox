alter table SHARD
    add column "router_canonical_hostname" varchar(255);

update SHARD
    set router_canonical_hostname='${shard-router-canonical-hostname}' where id='${shard-id}';

alter table SHARD
    alter column router_canonical_hostname set NOT NULL;