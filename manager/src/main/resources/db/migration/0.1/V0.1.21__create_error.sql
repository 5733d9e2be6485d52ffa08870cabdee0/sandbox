create sequence ERROR_ID_SEQUENCE;

create table ERROR
(
    id                 bigint       NOT NULL PRIMARY KEY,
    bridge_id          varchar(255) NOT NULL,
    recorded_at        timestamp    NOT NULL,
    headers            jsonb        NOT NULL,
    payload            jsonb        NOT NULL
);

create index ERROR_INDEX on ERROR(bridge_id, id);
