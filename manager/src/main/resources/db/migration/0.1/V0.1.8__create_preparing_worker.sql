create table PREPARINGWORKER
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    submitted_at       timestamp    NOT NULL,
    modified_at        timestamp,
    entity_id          varchar(255),
    worker_id          varchar(255),
    type               varchar(255),
    status             varchar(255),
    desired_status     varchar(255)
);

alter table BRIDGE add column desired_status varchar(255);
alter table PROCESSOR add column desired_status varchar(255);