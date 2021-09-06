create table BRIDGE
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    customer_id        varchar(255) NOT NULL,
    name               varchar(255) NOT NULL,
    submitted_at       timestamp    NOT NULL,
    published_at       timestamp,
    status             varchar(255),
    endpoint           varchar(255),
    unique (customer_id, name)
);