create table SHARD
(
    id      varchar(255) NOT NULL PRIMARY KEY,
    type    varchar(255) NOT NULL
);

INSERT INTO SHARD(id, type) VALUES ('${shard-id}', 'TRADITIONAL');

ALTER TABLE BRIDGE ADD shard_id varchar(255);
UPDATE BRIDGE SET shard_id='${shard-id}' FROM BRIDGE b;
ALTER TABLE BRIDGE ADD CONSTRAINT fk_shard FOREIGN KEY (shard_id) REFERENCES SHARD (id);

ALTER TABLE PROCESSOR ADD shard_id varchar(255);
UPDATE PROCESSOR SET shard_id='${shard-id}' FROM PROCESSOR p;
ALTER TABLE PROCESSOR ADD CONSTRAINT fk_shard FOREIGN KEY (shard_id) REFERENCES SHARD (id);
