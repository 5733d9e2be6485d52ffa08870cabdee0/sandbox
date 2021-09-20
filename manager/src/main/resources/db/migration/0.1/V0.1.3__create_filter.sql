create table FILTER
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    processor_id       varchar(255) NOT NULL,
    key                varchar(255) NOT NULL,
    type               varchar(255) NOT NULL,
    value              TEXT NOT NULL,
    constraint fk_processor foreign key (processor_id) references PROCESSOR (id)
);