-- add new "organisation_id" column
alter table BRIDGE add column organisation_id varchar(255);
update BRIDGE set organisation_id='';
alter table BRIDGE alter column organisation_id set NOT NULL;