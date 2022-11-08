-- This procedure by choice does not "commit" at the end to avoid clashing
-- with Hibernate transaction management.
create procedure cleanup_processing_error(max_errors_per_bridge integer)
    language plpgsql
as
$$
declare
    exceeding_bridge record;
begin
    for exceeding_bridge in
        select bridge_id from processing_error group by bridge_id having count(*) > max_errors_per_bridge
        loop
            delete
            from processing_error
            where bridge_id = exceeding_bridge.bridge_id
              and id <= (
                select id
                from processing_error
                where bridge_id = exceeding_bridge.bridge_id
                order by id desc
                limit 1 offset max_errors_per_bridge
            );
        end loop;
end
$$;
