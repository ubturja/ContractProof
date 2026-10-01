-- Dispute job linkage, optional complaint attachment, AI summary fields, attachment storage.

alter table public.disputes
    add column service_job_id uuid,
    add column disputed_service_job_requirement_id uuid,
    add column complaint_attachment_object_path text,
    add column complaint_attachment_mime_type text,
    add column ai_summary_json jsonb,
    add column ai_summary_generated_at timestamptz;

alter table public.disputes
    add constraint disputes_service_job_fkey
        foreign key (organization_id, service_job_id)
        references public.service_jobs (organization_id, id);

alter table public.disputes
    add constraint disputes_disputed_requirement_fkey
        foreign key (organization_id, service_job_id, disputed_service_job_requirement_id)
        references public.service_job_requirements (organization_id, service_job_id, id);

create index disputes_organization_id_service_job_id_idx
    on public.disputes (organization_id, service_job_id);

create or replace function private.freeze_dispute()
returns trigger
language plpgsql
as $$
begin
    if new.recorded_by is distinct from old.recorded_by
        or new.client_id is distinct from old.client_id
        or new.location_id is distinct from old.location_id
        or new.service_date is distinct from old.service_date
        or new.complaint is distinct from old.complaint
        or new.organization_id is distinct from old.organization_id
        or new.is_demo is distinct from old.is_demo
        or new.created_at is distinct from old.created_at
        or new.id is distinct from old.id
        or new.service_job_id is distinct from old.service_job_id
        or new.disputed_service_job_requirement_id is distinct from old.disputed_service_job_requirement_id
        or new.complaint_attachment_object_path is distinct from old.complaint_attachment_object_path
        or new.complaint_attachment_mime_type is distinct from old.complaint_attachment_mime_type
    then
        raise exception 'dispute content cannot be changed';
    end if;
    if old.status = 'closed' and new.status is distinct from 'closed' then
        raise exception 'closed dispute cannot be reopened';
    end if;
    if old.sync_status = 'uploaded' and new.sync_status is distinct from 'uploaded' then
        raise exception 'uploaded dispute cannot change sync status';
    end if;
    return new;
end;
$$;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'dispute-attachments',
    'dispute-attachments',
    false,
    10485760,
    array['application/pdf', 'image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do nothing;

create policy dispute_attachments_objects_select on storage.objects
for select to authenticated
using (
    bucket_id = 'dispute-attachments'
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

create policy dispute_attachments_objects_insert on storage.objects
for insert to authenticated
with check (
    bucket_id = 'dispute-attachments'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
        || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/.+$'
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy dispute_attachments_objects_update on storage.objects
for update to authenticated
using (
    bucket_id = 'dispute-attachments'
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and name ~ (
        '^'
        || (
            select membership.organization_id::text
            from private.current_membership() as membership
        )
        || '/'
    )
)
with check (
    bucket_id = 'dispute-attachments'
    and (select membership.role from private.current_membership() as membership) = 'owner'
);
