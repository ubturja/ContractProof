-- Client read access for completed service records and own disputes; dispute filing.

create policy service_jobs_select_client on public.service_jobs
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'client'
    and client_id = (select membership.client_id from private.current_membership() as membership)
    and status in ('completed', 'disputed')
);

create policy locations_select_client on public.locations
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'client'
    and exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = locations.organization_id
          and job.location_id = locations.id
          and job.client_id = (select membership.client_id from private.current_membership() as membership)
          and job.status in ('completed', 'disputed')
    )
);

create policy disputes_select_client on public.disputes
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'client'
    and client_id = (select membership.client_id from private.current_membership() as membership)
);

create policy disputes_insert_client on public.disputes
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'client'
    and client_id = (select membership.client_id from private.current_membership() as membership)
    and recorded_by = auth.uid()
    and exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = disputes.organization_id
          and job.id = disputes.service_job_id
          and job.client_id = disputes.client_id
          and job.location_id = disputes.location_id
          and job.status in ('completed', 'disputed')
    )
);

create policy dispute_items_insert_client on public.dispute_items
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'client'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = dispute_items.organization_id
          and dispute.id = dispute_items.dispute_id
          and dispute.client_id = (select membership.client_id from private.current_membership() as membership)
          and dispute.recorded_by = auth.uid()
    )
);

create or replace function private.mark_job_disputed_on_dispute_insert()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.service_job_id is not null then
        update public.service_jobs
        set status = 'disputed',
            updated_at = now()
        where organization_id = new.organization_id
          and id = new.service_job_id
          and status = 'completed';
    end if;
    return new;
end;
$$;

create trigger disputes_mark_job_disputed
after insert on public.disputes
for each row
execute function private.mark_job_disputed_on_dispute_insert();

create policy dispute_attachments_objects_insert_client on storage.objects
for insert to authenticated
with check (
    bucket_id = 'dispute-attachments'
    and (select membership.role from private.current_membership() as membership) = 'client'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/.+$'
    )
);
