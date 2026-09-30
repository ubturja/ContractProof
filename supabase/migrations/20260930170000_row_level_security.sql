-- Membership is the tenant boundary. This migration does not create or replace auth.uid().

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon nologin;
    end if;
end
$$;

create function private.current_membership()
returns table (
    organization_id uuid,
    role text,
    location_ids uuid[],
    client_id uuid
)
language sql
stable
security definer
set search_path = public
set row_security = off
as $$
    select
        member.organization_id,
        member.role,
        member.location_ids,
        member.client_id
    from public.organization_members as member
    where member.user_id = auth.uid();
$$;

create function private.can_claim_organization(target uuid)
returns boolean
language sql
stable
security definer
set search_path = public
set row_security = off
as $$
    select exists (
        select 1
        from public.organizations
        where id = target
          and created_by = auth.uid()
    )
    and not exists (
        select 1
        from public.organization_members
        where organization_id = target
          and role = 'owner'
    );
$$;

create function private.can_create_organization(target uuid)
returns boolean
language sql
stable
security definer
set search_path = public
set row_security = off
as $$
    select (select membership.organization_id from private.current_membership() as membership) is null
        and not exists (
            select 1
            from public.organizations
            where created_by = auth.uid()
              and id is distinct from target
        );
$$;

alter function private.touch_updated_at()
    security definer
    set search_path = public
    set row_security = off;

alter function private.current_actor()
    security definer
    set search_path = public
    set row_security = off;

alter function private.member_organization(uuid)
    security definer
    set search_path = public
    set row_security = off;

alter function private.reject_cross_tenant_user()
    security definer
    set search_path = public
    set row_security = off;

alter function private.member_locations_in_org()
    security definer
    set search_path = public
    set row_security = off;

alter function private.seed_free_subscription()
    security definer
    set search_path = public
    set row_security = off;

alter function private.on_contract_version_write()
    security definer
    set search_path = public
    set row_security = off;

alter function private.on_contract_version_approved()
    security definer
    set search_path = public
    set row_security = off;

alter function private.freeze_approved_requirements()
    security definer
    set search_path = public
    set row_security = off;

alter function private.schedule_matches_contract_location()
    security definer
    set search_path = public
    set row_security = off;

alter function private.on_service_job_write()
    security definer
    set search_path = public
    set row_security = off;

alter function private.on_service_job_requirement_write()
    security definer
    set search_path = public
    set row_security = off;

alter function private.on_evidence_record_write()
    security definer
    set search_path = public
    set row_security = off;

alter function private.freeze_evidence_file()
    security definer
    set search_path = public
    set row_security = off;

alter function private.freeze_exception()
    security definer
    set search_path = public
    set row_security = off;

alter function private.freeze_dispute()
    security definer
    set search_path = public
    set row_security = off;

alter function private.dispute_item_is_uploaded()
    security definer
    set search_path = public
    set row_security = off;

alter function private.freeze_report()
    security definer
    set search_path = public
    set row_security = off;

alter function private.audit_event()
    security definer
    set search_path = public, private
    set row_security = off;

revoke all on all functions in schema private from public;
revoke all on all functions in schema private from anon;
revoke all on all functions in schema private from authenticated;

grant usage on schema private to authenticated;

grant execute on function
    private.current_membership(),
    private.can_claim_organization(uuid),
    private.can_create_organization(uuid)
to authenticated;

-- PostgreSQL checks execute privilege before a trigger runs.
-- These functions return trigger and cannot be called as queries.
grant execute on function
    private.touch_updated_at(),
    private.reject_cross_tenant_user(),
    private.member_locations_in_org(),
    private.seed_free_subscription(),
    private.on_contract_version_write(),
    private.on_contract_version_approved(),
    private.freeze_approved_requirements(),
    private.schedule_matches_contract_location(),
    private.on_service_job_write(),
    private.on_service_job_requirement_write(),
    private.on_evidence_record_write(),
    private.freeze_evidence_file(),
    private.freeze_exception(),
    private.freeze_dispute(),
    private.dispute_item_is_uploaded(),
    private.freeze_report(),
    private.audit_event()
to authenticated;

do $$
declare
    business_table text;
begin
    foreach business_table in array array[
        'organizations',
        'users',
        'organization_members',
        'clients',
        'locations',
        'subscriptions',
        'contracts',
        'contract_versions',
        'contract_requirements',
        'service_schedules',
        'service_jobs',
        'service_job_requirements',
        'evidence_records',
        'evidence_files',
        'exceptions',
        'disputes',
        'dispute_items',
        'reports',
        'audit_logs'
    ]
    loop
        execute format('alter table public.%I enable row level security', business_table);
        execute format('alter table public.%I force row level security', business_table);
    end loop;
end
$$;

revoke all on all tables in schema public from public;
revoke all on all tables in schema public from anon;
revoke all on all tables in schema public from authenticated;

grant select, insert, update on table
    public.organizations,
    public.users,
    public.organization_members,
    public.clients,
    public.locations,
    public.contracts,
    public.contract_versions,
    public.contract_requirements,
    public.service_schedules,
    public.service_jobs,
    public.service_job_requirements,
    public.evidence_records,
    public.evidence_files,
    public.exceptions,
    public.disputes,
    public.dispute_items,
    public.reports
to authenticated;

grant select on table
    public.subscriptions,
    public.audit_logs
to authenticated;

revoke insert, update, delete on table
    public.subscriptions,
    public.audit_logs
from authenticated;

revoke delete on table
    public.evidence_records,
    public.evidence_files,
    public.exceptions,
    public.dispute_items,
    public.reports
from authenticated;

create policy organizations_select on public.organizations
for select to authenticated
using (
    (
        id = (select membership.organization_id from private.current_membership() as membership)
        and (select membership.role from private.current_membership() as membership) in ('owner', 'manager', 'cleaner')
    )
    or created_by = auth.uid()
);

create policy organizations_insert on public.organizations
for insert to authenticated
with check (
    created_by = auth.uid()
    and private.can_create_organization(id)
);

create policy organizations_update on public.organizations
for update to authenticated
using (
    id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy users_select on public.users
for select to authenticated
using (
    id = auth.uid()
    or (
        (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
        and exists (
            select 1
            from public.organization_members as member
            where member.user_id = users.id
              and member.organization_id = (select membership.organization_id from private.current_membership() as membership)
        )
    )
);

create policy users_insert on public.users
for insert to authenticated
with check (id = auth.uid());

create policy users_update on public.users
for update to authenticated
using (
    id = auth.uid()
    and coalesce((select membership.role from private.current_membership() as membership), '') <> 'client'
)
with check (id = auth.uid());

create policy organization_members_select on public.organization_members
for select to authenticated
using (
    user_id = auth.uid()
    or (
        organization_id = (select membership.organization_id from private.current_membership() as membership)
        and (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
    )
);

create policy organization_members_insert on public.organization_members
for insert to authenticated
with check (
    (
        user_id = auth.uid()
        and role = 'owner'
        and (select membership.organization_id from private.current_membership() as membership) is null
        and private.can_claim_organization(organization_id)
    )
    or (
        (select membership.role from private.current_membership() as membership) = 'owner'
        and organization_id = (select membership.organization_id from private.current_membership() as membership)
        and user_id <> auth.uid()
    )
);

create policy organization_members_update on public.organization_members
for update to authenticated
using (
    (select membership.role from private.current_membership() as membership) = 'owner'
    and organization_id = (select membership.organization_id from private.current_membership() as membership)
    and user_id <> auth.uid()
)
with check (
    (select membership.role from private.current_membership() as membership) = 'owner'
    and organization_id = (select membership.organization_id from private.current_membership() as membership)
    and user_id <> auth.uid()
);

create policy clients_select on public.clients
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
);

create policy clients_insert on public.clients
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy clients_update on public.clients
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy locations_select on public.locations
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'manager'
            and exists (
                select 1
                from private.current_membership() as membership
                where id = any (membership.location_ids)
            )
        )
    )
);

create policy locations_insert on public.locations
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy locations_update on public.locations
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy subscriptions_select on public.subscriptions
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contracts_select on public.contracts
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'manager'
            and exists (
                select 1
                from private.current_membership() as membership
                where location_id = any (membership.location_ids)
            )
        )
    )
);

create policy contracts_insert on public.contracts
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contracts_update on public.contracts
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contract_versions_select on public.contract_versions
for select to authenticated
using (
    exists (
        select 1
        from public.contracts as contract
        where contract.organization_id = contract_versions.organization_id
          and contract.id = contract_versions.contract_id
    )
);

create policy contract_versions_insert on public.contract_versions
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contracts as contract
        where contract.organization_id = contract_versions.organization_id
          and contract.id = contract_versions.contract_id
    )
);

create policy contract_versions_update on public.contract_versions
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contracts as contract
        where contract.organization_id = contract_versions.organization_id
          and contract.id = contract_versions.contract_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contracts as contract
        where contract.organization_id = contract_versions.organization_id
          and contract.id = contract_versions.contract_id
    )
);

create policy contract_requirements_select on public.contract_requirements
for select to authenticated
using (
    exists (
        select 1
        from public.contract_versions as version
        where version.organization_id = contract_requirements.organization_id
          and version.id = contract_requirements.contract_version_id
    )
);

create policy contract_requirements_insert on public.contract_requirements
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contract_versions as version
        where version.organization_id = contract_requirements.organization_id
          and version.id = contract_requirements.contract_version_id
    )
);

create policy contract_requirements_update on public.contract_requirements
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contract_versions as version
        where version.organization_id = contract_requirements.organization_id
          and version.id = contract_requirements.contract_version_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contract_versions as version
        where version.organization_id = contract_requirements.organization_id
          and version.id = contract_requirements.contract_version_id
    )
);

create policy service_schedules_select on public.service_schedules
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'manager'
            and exists (
                select 1
                from private.current_membership() as membership
                where location_id = any (membership.location_ids)
            )
        )
    )
);

create policy service_schedules_insert on public.service_schedules
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy service_schedules_update on public.service_schedules
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy service_jobs_select on public.service_jobs
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'manager'
            and exists (
                select 1
                from private.current_membership() as membership
                where location_id = any (membership.location_ids)
            )
        )
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and assigned_user_id = auth.uid()
        )
    )
);

create policy service_jobs_insert on public.service_jobs
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy service_jobs_update on public.service_jobs
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and assigned_user_id = auth.uid()
        )
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and assigned_user_id = auth.uid()
        )
    )
);

create policy service_job_requirements_select on public.service_job_requirements
for select to authenticated
using (
    exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = service_job_requirements.organization_id
          and job.id = service_job_requirements.service_job_id
    )
);

create policy service_job_requirements_insert on public.service_job_requirements
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = service_job_requirements.organization_id
          and job.id = service_job_requirements.service_job_id
    )
);

create policy service_job_requirements_update on public.service_job_requirements
for update to authenticated
using (
    (select membership.role from private.current_membership() as membership) in ('owner', 'cleaner')
    and exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = service_job_requirements.organization_id
          and job.id = service_job_requirements.service_job_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) in ('owner', 'cleaner')
    and exists (
        select 1
        from public.service_jobs as job
        where job.organization_id = service_job_requirements.organization_id
          and job.id = service_job_requirements.service_job_id
    )
);

create policy evidence_records_select on public.evidence_records
for select to authenticated
using (
    exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = evidence_records.organization_id
          and requirement.service_job_id = evidence_records.service_job_id
          and requirement.id = evidence_records.service_job_requirement_id
    )
);

create policy evidence_records_insert on public.evidence_records
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and captured_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = evidence_records.organization_id
          and requirement.service_job_id = evidence_records.service_job_id
          and requirement.id = evidence_records.service_job_requirement_id
    )
);

create policy evidence_records_update on public.evidence_records
for update to authenticated
using (
    (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and captured_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = evidence_records.organization_id
          and requirement.service_job_id = evidence_records.service_job_id
          and requirement.id = evidence_records.service_job_requirement_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and captured_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = evidence_records.organization_id
          and requirement.service_job_id = evidence_records.service_job_id
          and requirement.id = evidence_records.service_job_requirement_id
    )
);

create policy evidence_files_select on public.evidence_files
for select to authenticated
using (
    exists (
        select 1
        from public.evidence_records as record
        where record.organization_id = evidence_files.organization_id
          and record.id = evidence_files.evidence_record_id
    )
);

create policy evidence_files_insert on public.evidence_files
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) in ('owner', 'cleaner')
    and exists (
        select 1
        from public.evidence_records as record
        where record.organization_id = evidence_files.organization_id
          and record.id = evidence_files.evidence_record_id
    )
);

create policy evidence_files_update on public.evidence_files
for update to authenticated
using (
    (select membership.role from private.current_membership() as membership) in ('owner', 'cleaner')
    and exists (
        select 1
        from public.evidence_records as record
        where record.organization_id = evidence_files.organization_id
          and record.id = evidence_files.evidence_record_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) in ('owner', 'cleaner')
    and exists (
        select 1
        from public.evidence_records as record
        where record.organization_id = evidence_files.organization_id
          and record.id = evidence_files.evidence_record_id
    )
);

create policy exceptions_select on public.exceptions
for select to authenticated
using (
    exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = exceptions.organization_id
          and requirement.service_job_id = exceptions.service_job_id
          and requirement.id = exceptions.service_job_requirement_id
    )
);

create policy exceptions_insert on public.exceptions
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and recorded_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = exceptions.organization_id
          and requirement.service_job_id = exceptions.service_job_id
          and requirement.id = exceptions.service_job_requirement_id
    )
);

create policy exceptions_update on public.exceptions
for update to authenticated
using (
    (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and recorded_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = exceptions.organization_id
          and requirement.service_job_id = exceptions.service_job_id
          and requirement.id = exceptions.service_job_requirement_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'cleaner'
            and recorded_by = auth.uid()
        )
    )
    and exists (
        select 1
        from public.service_job_requirements as requirement
        where requirement.organization_id = exceptions.organization_id
          and requirement.service_job_id = exceptions.service_job_id
          and requirement.id = exceptions.service_job_requirement_id
    )
);

create policy disputes_select on public.disputes
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (
        (select membership.role from private.current_membership() as membership) = 'owner'
        or (
            (select membership.role from private.current_membership() as membership) = 'manager'
            and exists (
                select 1
                from private.current_membership() as membership
                where location_id = any (membership.location_ids)
            )
        )
    )
);

create policy disputes_insert on public.disputes
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy disputes_update on public.disputes
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy dispute_items_select on public.dispute_items
for select to authenticated
using (
    exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = dispute_items.organization_id
          and dispute.id = dispute_items.dispute_id
    )
);

create policy dispute_items_insert on public.dispute_items
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = dispute_items.organization_id
          and dispute.id = dispute_items.dispute_id
    )
);

create policy dispute_items_update on public.dispute_items
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = dispute_items.organization_id
          and dispute.id = dispute_items.dispute_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = dispute_items.organization_id
          and dispute.id = dispute_items.dispute_id
    )
);

create policy reports_select on public.reports
for select to authenticated
using (
    exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = reports.organization_id
          and dispute.id = reports.dispute_id
    )
);

create policy reports_insert on public.reports
for insert to authenticated
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = reports.organization_id
          and dispute.id = reports.dispute_id
    )
);

create policy reports_update on public.reports
for update to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = reports.organization_id
          and dispute.id = reports.dispute_id
    )
)
with check (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.disputes as dispute
        where dispute.organization_id = reports.organization_id
          and dispute.id = reports.dispute_id
    )
);

create policy audit_logs_select on public.audit_logs
for select to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
);
