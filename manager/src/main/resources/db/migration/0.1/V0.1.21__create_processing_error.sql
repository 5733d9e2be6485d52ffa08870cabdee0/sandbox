create sequence PROCESSING_ERROR_ID_SEQUENCE;

create table PROCESSING_ERROR
(
    id                 bigint       NOT NULL PRIMARY KEY,
    bridge_id          varchar(255) NOT NULL,
    recorded_at        timestamp    NOT NULL,
    headers            jsonb        NOT NULL,
    payload            jsonb
);

create index PROCESSING_ERROR_INDEX on PROCESSING_ERROR(bridge_id, id);
