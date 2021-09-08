create table PROCESSOR
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    bridge_id          varchar(255) NOT NULL,
    name               varchar(255) NOT NULL,
    submitted_at       timestamp    NOT NULL,
    published_at       timestamp,
    status             varchar(255),
    version            integer NOT NULL default 0,
    unique (bridge_id, name),
    constraint fk_bridge foreign key (bridge_id) references BRIDGE (id)
);