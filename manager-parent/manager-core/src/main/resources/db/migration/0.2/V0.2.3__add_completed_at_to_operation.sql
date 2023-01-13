ALTER TABLE BRIDGE_V2
    ADD COLUMN operation_completed_at timestamp;

UPDATE BRIDGE_V2
SET operation_completed_at = now()
WHERE id IN
      (
          SELECT b.id
          FROM BRIDGE_V2 b
                   LEFT JOIN (
              SELECT bridge_id,
                     count(*) AS incomplete_count
              FROM CONDITION c
              WHERE status != 'TRUE'
              GROUP BY c.bridge_id) cp
                             ON b.id = cp.bridge_id
          WHERE (cp.incomplete_count = 0 OR cp.incomplete_count IS NULL)
      );

ALTER TABLE PROCESSOR_V2
    ADD COLUMN operation_completed_at timestamp;

UPDATE PROCESSOR_V2
SET operation_completed_at = now()
WHERE id IN
      (
          SELECT b.id
          FROM PROCESSOR_V2 b
                   LEFT JOIN (
              SELECT processor_id,
                     count(*) AS incomplete_count
              FROM CONDITION c
              WHERE status != 'TRUE'
              GROUP BY c.processor_id) cp
                             ON b.id = cp.processor_id
          WHERE (cp.incomplete_count = 0 OR cp.incomplete_count IS NULL)
      );
