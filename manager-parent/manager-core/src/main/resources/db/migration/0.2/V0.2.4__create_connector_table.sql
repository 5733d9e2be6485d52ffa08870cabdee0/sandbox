CREATE TABLE CONNECTOR_V2 (
    id                             varchar(255) NOT NULL PRIMARY KEY,
    name                           varchar(255) NOT NULL,
    type                           varchar(255) NOT NULL, -- The discriminator for sources/sinks
    connector_type_id              varchar(255) NOT NULL,
    connector_external_id          varchar(255),
    topic_name                     varchar(255),
    error                          text,

    bridge_id                      varchar(255) NOT NULL,

    submitted_at                   timestamp    NOT NULL,
    published_at                   timestamp,

    operation_type                 varchar(255) NOT NULL,
    operation_requested_at         timestamp    NOT NULL,
    operation_completed_at         timestamp,

    owner                          varchar(255) NOT NULL,

    definition                     jsonb        NOT NULL,

    version                        integer      NOT NULL default 0,
    generation                     integer      NOT NULL default 0,

    unique (bridge_id, name, type),
    constraint fk_processor foreign key (bridge_id) references BRIDGE_V2 (id)
);

alter table CONDITION add column "connector_id" varchar(255);
alter table CONDITION add foreign key (connector_id) references CONNECTOR_V2(id);
