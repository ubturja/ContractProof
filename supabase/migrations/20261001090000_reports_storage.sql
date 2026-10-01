-- Private dispute evidence PDF reports bucket.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'reports',
    'reports',
    false,
    20971520,
    array['application/pdf']
)
on conflict (id) do nothing;

create policy reports_objects_select on storage.objects
for select to authenticated
using (
    bucket_id = 'reports'
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
