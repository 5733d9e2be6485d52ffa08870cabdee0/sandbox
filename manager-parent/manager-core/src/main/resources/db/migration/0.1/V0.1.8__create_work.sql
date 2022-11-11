create table WORK
(
    id                  varchar(255) NOT NULL PRIMARY KEY,
    managed_resource_id varchar(255) NOT NULL,
    type                varchar(255) NOT NULL,
    worker_id           varchar(255) NOT NULL,
    submitted_at        timestamp    NOT NULL,
    modified_at         timestamp    NOT NULL,
    attempts            integer      NOT NULL default 0,
    version             integer      NOT NULL default 0,
    unique (managed_resource_id, worker_id)
);

alter table BRIDGE
    add column version integer NOT NULL default 0;
alter table BRIDGE
    add column dependency_status varchar(255);
alter table BRIDGE
    add column modified_at timestamp;

alter table PROCESSOR
    add column dependency_status varchar(255);
alter table PROCESSOR
    add column modified_at timestamp;

alter table CONNECTOR
    add column dependency_status varchar(255);

alter table CONNECTOR drop column desired_state;
alter table CONNECTOR drop column worker_id;

-- This will not cater for any Processor that was in the midst of being provisioned/de-provisioned
update PROCESSOR
set dependency_status = 'READY'
where status = 'READY';

-- This will not cater for any Connector that was in the midst of being provisioned/de-provisioned
update CONNECTOR
set dependency_status = 'READY'
where status = 'READY';
