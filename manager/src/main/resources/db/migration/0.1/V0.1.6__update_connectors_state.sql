alter table CONNECTOR add column desired_state varchar(255);
alter table CONNECTOR add column connector_type varchar(255);
alter table CONNECTOR add column worker_id varchar(255);
alter table CONNECTOR add column topic_name varchar(255);
alter table CONNECTOR add column error text;
alter table CONNECTOR add column modified_at timestamp;

UPDATE CONNECTOR SET status='READY' WHERE status='AVAILABLE';
UPDATE CONNECTOR SET status='ACCEPTED' WHERE status='REQUESTED';