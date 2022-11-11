alter table PROCESSING_ERROR
add foreign key (bridge_id)
references BRIDGE(id)
on delete cascade;
