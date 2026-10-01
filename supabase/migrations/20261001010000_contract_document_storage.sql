-- Private contract PDFs: org-prefixed paths, file metadata, owner-only writes.

alter table public.contract_versions
    add column original_file_name text,
    add column mime_type text,
    add column byte_size bigint;

alter table public.contract_versions
    drop constraint version_file_pair;

alter table public.contract_versions
    add constraint version_file_pair check (
        (
            bucket is null
            and object_path is null
            and original_file_name is null
            and mime_type is null
            and byte_size is null
        )
        or (
            bucket = 'contracts'
            and object_path = organization_id::text || '/' || contract_id::text || '/' || id::text || '.pdf'
            and original_file_name is not null
            and char_length(original_file_name) > 0
            and mime_type = 'application/pdf'
            and byte_size > 0
        )
    );

drop policy if exists contracts_objects_select on storage.objects;
drop policy if exists contracts_objects_insert on storage.objects;
drop policy if exists contracts_objects_update on storage.objects;

create policy contracts_objects_select on storage.objects
for select to authenticated
using (
    bucket_id = 'contracts'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$'
    )
    and (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
);

create policy contracts_objects_insert on storage.objects
for insert to authenticated
with check (
    bucket_id = 'contracts'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$'
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contracts_objects_update on storage.objects
for update to authenticated
using (
    bucket_id = 'contracts'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$'
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    bucket_id = 'contracts'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$'
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contracts_objects_delete on storage.objects
for delete to authenticated
using (
    bucket_id = 'contracts'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$'
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);
