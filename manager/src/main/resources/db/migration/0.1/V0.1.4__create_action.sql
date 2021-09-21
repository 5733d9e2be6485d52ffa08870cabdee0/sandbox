create table ACTION
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    processor_id       varchar(255),
    name               varchar(255) NOT NULL,
    type               varchar(255) NOT NULL,
    unique (processor_id, name),
    constraint fk_action_processor foreign key (processor_id) references PROCESSOR(id)
);

create table ACTION_PARAMETER
(
    name        varchar(255) NOT NULL,
    value       varchar(255) NOT NULL,
    action_id   varchar(255),
    constraint  fk_action foreign key(action_id) references ACTION(id)
);

alter table PROCESSOR add column action_id varchar(255) NOT NULL;