create table WORK_ERRORS
(
    id                  varchar(255) NOT NULL PRIMARY KEY,
    managed_resource_id varchar(255) NOT NULL,
    code                varchar(255) NOT NULL,
    reason              varchar(255) NOT NULL,
    type                varchar(255) NOT NULL,
    timestamp           timestamp    NOT NULL,
    unique (id, managed_resource_id)
);