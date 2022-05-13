alter table BRIDGE add column deletion_requested_at timestamp;
alter table PROCESSOR add column deletion_requested_at timestamp;
alter table CONNECTOR add column deletion_requested_at timestamp;
