CREATE TABLE BRIDGE_V2 (
    id                 varchar(255) NOT NULL PRIMARY KEY,

    customer_id        varchar(255) NOT NULL,
    organisation_id    varchar(255) NOT NULL,
    owner              varchar(255) NOT NULL,
    subscription_id    varchar(255) NOT NULL,

    name               varchar(255) NOT NULL,
    cloud_provider     varchar(255) NOT NULL,
    region             varchar(255) NOT NULL,

    endpoint           varchar(255),
    shard_id           varchar(255) NOT NULL,

    submitted_at       timestamp    NOT NULL,
    published_at       timestamp,

    error_id           integer,
    error_uuid         varchar(255),

    version            integer      NOT NULL default 0,
    generation         integer      NOT NULL default 0,

    unique (customer_id, name)
);


CREATE TABLE PROCESSOR_V2 (
    id                 varchar(255) NOT NULL PRIMARY KEY,

    name               varchar(255) NOT NULL,
    flows              jsonb        NOT NULL,

    owner              varchar(255) NOT NULL,

    submitted_at       timestamp    NOT NULL,
    published_at       timestamp,

    version            integer      NOT NULL default 0,
    generation         integer      NOT NULL default 0,

    error_id           integer,
    error_uuid         varchar(255),

    bridge_id          varchar(255) NOT NULL,

    unique (bridge_id, name),
    constraint fk_bridge foreign key (bridge_id) references BRIDGE_V2 (id)
);

CREATE TABLE OPERATION (
    id                   varchar(255)   NOT NULL PRIMARY KEY,
    type                 varchar(255)   NOT NULL,
    requested_at         timestamp      NOT NULL,

    managed_resource_id  varchar(255)
);

CREATE INDEX managed_resource_id_index ON OPERATION (managed_resource_id);

CREATE TABLE CONDITION (
    id                     varchar(255) NOT NULL PRIMARY KEY,
    type                   varchar(255) NOT NULL,
    status                 varchar(255) NOT NULL,
    last_transition_time   timestamp    NOT NULL,
    reason                 varchar(255),
    message                varchar(255),
    errorCode              varchar(255),
    component              varchar(255) NOT NULL,

    operation_id           varchar(255) NOT NULL,

    constraint fk_operation foreign key (operation_id) references OPERATION (id)
);