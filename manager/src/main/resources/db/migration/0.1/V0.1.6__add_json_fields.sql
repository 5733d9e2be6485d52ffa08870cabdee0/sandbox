alter table PROCESSOR drop column action_id;
alter table PROCESSOR drop column transformation_template;

drop table ACTION_PARAMETER;
drop table ACTION;
drop table FILTER;

alter table PROCESSOR add column definition jsonb NOT NULL;
