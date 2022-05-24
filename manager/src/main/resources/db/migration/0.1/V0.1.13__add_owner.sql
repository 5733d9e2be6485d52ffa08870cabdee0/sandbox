-- add new "owner" column
alter table BRIDGE add column owner varchar(255);
update BRIDGE set owner='';
alter table BRIDGE alter column owner set NOT NULL;

alter table PROCESSOR add column owner varchar(255);
update PROCESSOR set owner='';
alter table PROCESSOR alter column owner set NOT NULL;
