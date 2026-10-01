-- Private evidence photos: org-prefixed paths; cleaners upload on assigned jobs.

create policy evidence_objects_select on storage.objects
for select to authenticated
using (
    bucket_id = 'evidence'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    )
);

create policy evidence_objects_insert on storage.objects
for insert to authenticated
with check (
    bucket_id = 'evidence'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    )
    and (
        (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
        or exists (
            select 1
            from public.service_jobs as job
            where job.organization_id = (select membership.organization_id from private.current_membership() as membership)
              and job.assigned_to = auth.uid()
              and job.id::text = split_part(name, '/', 2)
        )
    )
);

create policy evidence_objects_update on storage.objects
for update to authenticated
using (
    bucket_id = 'evidence'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    )
)
with check (
    bucket_id = 'evidence'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    )
    and (
        (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
        or exists (
            select 1
            from public.service_jobs as job
            where job.organization_id = (select membership.organization_id from private.current_membership() as membership)
              and job.assigned_to = auth.uid()
              and job.id::text = split_part(name, '/', 2)
        )
    )
);
