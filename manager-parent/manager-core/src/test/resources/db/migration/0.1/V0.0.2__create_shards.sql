create table SHARD
(
    id                        varchar(255) NOT NULL PRIMARY KEY,
    router_canonical_hostname varchar(255)
);

INSERT INTO SHARD
VALUES ('${shard-id}', '${shard-router-canonical-hostname}');
