CREATE TABLE BRIDGE_V2 (
    id                             varchar(255) NOT NULL PRIMARY KEY,

    customer_id                    varchar(255) NOT NULL,
    organisation_id                varchar(255) NOT NULL,
    owner                          varchar(255) NOT NULL,
    subscription_id                varchar(255) NOT NULL,

    name                           varchar(255) NOT NULL,
    cloud_provider                 varchar(255) NOT NULL,
    region                         varchar(255) NOT NULL,

    endpoint                       varchar(255),
    shard_id                       varchar(255) NOT NULL,

    submitted_at                   timestamp    NOT NULL,
    published_at                   timestamp,

    operation_type                 varchar(255) NOT NULL,
    operation_requested_at         timestamp    NOT NULL,

    version                        integer      NOT NULL default 0,
    generation                     integer      NOT NULL default 0,

    unique (customer_id, name)
);


CREATE TABLE PROCESSOR_V2 (
    id                             varchar(255) NOT NULL PRIMARY KEY,

    name                           varchar(255) NOT NULL,
    flows                          jsonb        NOT NULL,

    owner                          varchar(255) NOT NULL,

    submitted_at                   timestamp    NOT NULL,
    published_at                   timestamp,

    version                        integer      NOT NULL default 0,
    generation                     integer      NOT NULL default 0,

    bridge_id                      varchar(255) NOT NULL,

    operation_type                 varchar(255) NOT NULL,
    operation_requested_at         timestamp    NOT NULL,

    unique (bridge_id, name),
    constraint fk_bridge foreign key (bridge_id) references BRIDGE_V2 (id)
);

CREATE TABLE CONDITION (
    id                     varchar(255) NOT NULL PRIMARY KEY,
    type                   varchar(255) NOT NULL,
    status                 varchar(255) NOT NULL,
    last_transition_time   timestamp    NOT NULL,
    reason                 varchar(255),
    message                varchar(255),
    errorCode              varchar(255),
    component              varchar(255) NOT NULL,

    bridge_id              varchar(255),
    processor_id           varchar(255),

    constraint fk_bridge foreign key (bridge_id) references BRIDGE_V2 (id),
    constraint fk_processor foreign key (processor_id) references PROCESSOR_V2 (id)
);