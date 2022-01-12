create table CONNECTOR
(
    id                          varchar(255) NOT NULL PRIMARY KEY,
    processor_id                varchar(255) NOT NULL,
    name                        varchar(255) NOT NULL,
    submitted_at                timestamp    NOT NULL,
    published_at                timestamp,
    status                      varchar(255),
    version                     integer      NOT NULL default 0,
    definition                  jsonb        NOT NULL,
    unique (processor_id, name),
    constraint fk_processor foreign key (processor_id) references PROCESSOR (id)
);