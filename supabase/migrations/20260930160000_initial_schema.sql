-- ContractProof MVP schema. Row-level security is intentionally not enabled here.

create schema if not exists private;

revoke all on schema private from public;

create table public.organizations (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint organization_name_present check (char_length(name) > 0)
);

create table public.users (
    id uuid primary key references auth.users (id) on delete cascade,
    email text not null,
    display_name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint user_email_present check (char_length(email) > 0),
    constraint user_display_name_present check (char_length(display_name) > 0),
    constraint users_email_key unique (email)
);

alter table public.organizations
    add column created_by uuid not null references public.users (id);

create index organizations_created_by_idx on public.organizations (created_by);

create table public.organization_members (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    user_id uuid not null references public.users (id),
    role text not null,
    client_id uuid,
    location_ids uuid[] not null default '{}',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint member_role_known check (role in ('owner', 'manager', 'cleaner', 'client')),
    constraint member_client_link check (
        (role = 'client' and client_id is not null and cardinality(location_ids) = 0)
        or (role = 'owner' and client_id is null and cardinality(location_ids) = 0)
        or (role = 'manager' and client_id is null)
        or (role = 'cleaner' and client_id is null and cardinality(location_ids) = 0)
    ),
    constraint organization_members_organization_id_id_key unique (organization_id, id),
    constraint one_organization_per_user unique (user_id),
    constraint organization_members_organization_id_user_id_key unique (organization_id, user_id)
);

create index organization_members_location_ids_idx
    on public.organization_members using gin (location_ids);

create table public.clients (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    name text not null,
    status text not null default 'active',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint client_name_present check (char_length(name) > 0),
    constraint client_status_known check (status in ('active', 'archived')),
    constraint clients_organization_id_id_key unique (organization_id, id)
);

create index clients_organization_id_status_idx on public.clients (organization_id, status);

alter table public.organization_members
    add constraint organization_members_client_id_fkey
    foreign key (organization_id, client_id) references public.clients (organization_id, id);

create table public.locations (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    name text not null,
    timezone text not null,
    address text,
    status text not null default 'active',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint location_name_present check (char_length(name) > 0),
    constraint location_timezone_present check (char_length(timezone) > 0),
    constraint location_status_known check (status in ('active', 'archived')),
    constraint locations_organization_id_id_key unique (organization_id, id)
);

create index locations_organization_id_status_idx on public.locations (organization_id, status);

create table public.subscriptions (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    plan text not null default 'free',
    status text not null default 'active',
    revenuecat_app_user_id text,
    revenuecat_entitlement_id text,
    current_period_ends_at timestamptz,
    last_event_id text,
    source text not null default 'seed',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint subscription_plan_known check (plan in ('free', 'pro', 'business')),
    constraint subscription_status_known check (status in ('active', 'expired', 'cancelled', 'billing_issue')),
    constraint subscription_source_known check (source in ('webhook', 'seed')),
    constraint one_subscription_per_organization unique (organization_id),
    constraint subscription_event_once unique (last_event_id),
    constraint subscriptions_organization_id_id_key unique (organization_id, id)
);

create table public.contracts (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    client_id uuid not null,
    location_id uuid not null,
    title text not null,
    status text not null default 'draft',
    current_version_id uuid,
    is_demo boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint contract_title_present check (char_length(title) > 0),
    constraint contract_status_known check (status in ('draft', 'active', 'ended')),
    constraint contract_current_version_agrees check (
        (status = 'draft' and current_version_id is null)
        or (status = 'active' and current_version_id is not null)
        or status = 'ended'
    ),
    constraint contracts_organization_id_id_key unique (organization_id, id),
    constraint contracts_client_id_fkey
        foreign key (organization_id, client_id) references public.clients (organization_id, id),
    constraint contracts_location_id_fkey
        foreign key (organization_id, location_id) references public.locations (organization_id, id)
);

create index contracts_organization_id_status_idx on public.contracts (organization_id, status);
create index contracts_organization_id_client_id_idx on public.contracts (organization_id, client_id);
create index contracts_organization_id_location_id_idx on public.contracts (organization_id, location_id);

create table public.contract_versions (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    contract_id uuid not null,
    version_number integer not null,
    status text not null default 'uploaded',
    bucket text,
    object_path text,
    extraction jsonb,
    approved_at timestamptz,
    approved_by uuid references public.users (id),
    is_demo boolean not null default false,
    created_at timestamptz not null default now(),
    constraint version_number_positive check (version_number >= 1),
    constraint version_status_known check (status in ('uploaded', 'extracted', 'approved', 'superseded')),
    constraint version_file_pair check (
        (
            bucket is null
            and object_path is null
        )
        or (
            bucket = 'contracts'
            and object_path is not null
            and char_length(object_path) > 0
        )
    ),
    constraint version_approval_pair check (
        (
            status in ('approved', 'superseded')
            and approved_at is not null
            and approved_by is not null
        )
        or (
            status in ('uploaded', 'extracted')
            and approved_at is null
            and approved_by is null
        )
    ),
    constraint contract_versions_organization_id_id_key unique (organization_id, id),
    constraint contract_versions_organization_id_contract_id_id_key unique (organization_id, contract_id, id),
    constraint one_number_per_contract unique (contract_id, version_number),
    constraint contract_versions_contract_id_fkey
        foreign key (organization_id, contract_id) references public.contracts (organization_id, id)
);

create unique index one_approved_version
    on public.contract_versions (organization_id, contract_id)
    where status = 'approved';

create unique index one_version_in_review
    on public.contract_versions (organization_id, contract_id)
    where status in ('uploaded', 'extracted');

create index contract_versions_org_contract_version_idx
    on public.contract_versions (organization_id, contract_id, version_number);

create index contract_versions_approved_by_idx on public.contract_versions (approved_by);

alter table public.contracts
    add constraint contracts_current_version_id_fkey
    foreign key (organization_id, id, current_version_id)
    references public.contract_versions (organization_id, contract_id, id);

create table public.contract_requirements (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    contract_version_id uuid not null,
    sort_order integer not null,
    requirement_text text not null,
    requires_photo boolean not null,
    is_mandatory boolean not null default true,
    source text not null,
    extraction_key text,
    created_at timestamptz not null default now(),
    constraint requirement_sort_nonnegative check (sort_order >= 0),
    constraint requirement_text_present check (char_length(requirement_text) > 0),
    constraint requirement_source_known check (source in ('manual', 'extraction')),
    constraint contract_requirements_organization_id_id_key unique (organization_id, id),
    constraint contract_requirements_version_id_key unique (organization_id, contract_version_id, id),
    constraint requirement_order_once unique (contract_version_id, sort_order),
    constraint contract_requirements_version_id_fkey
        foreign key (organization_id, contract_version_id)
        references public.contract_versions (organization_id, id)
);

create index contract_requirements_organization_id_version_sort_idx
    on public.contract_requirements (organization_id, contract_version_id, sort_order);

create table public.service_schedules (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    contract_id uuid not null,
    location_id uuid not null,
    weekday smallint not null,
    start_time time not null,
    end_time time not null,
    timezone text not null,
    starts_on date not null,
    ends_on date,
    status text not null default 'active',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint schedule_weekday_iso check (weekday between 1 and 7),
    constraint schedule_timezone_present check (char_length(timezone) > 0),
    constraint schedule_time_order check (end_time > start_time),
    constraint schedule_date_order check (ends_on is null or ends_on >= starts_on),
    constraint schedule_status_known check (status in ('active', 'paused', 'ended')),
    constraint service_schedules_organization_id_id_key unique (organization_id, id),
    constraint service_schedules_contract_id_fkey
        foreign key (organization_id, contract_id) references public.contracts (organization_id, id),
    constraint service_schedules_location_id_fkey
        foreign key (organization_id, location_id) references public.locations (organization_id, id)
);

create index service_schedules_organization_id_contract_id_idx
    on public.service_schedules (organization_id, contract_id);

create index service_schedules_organization_id_status_idx
    on public.service_schedules (organization_id, status);

create table public.service_jobs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    location_id uuid not null,
    client_id uuid not null,
    contract_id uuid not null,
    contract_version_id uuid not null,
    schedule_id uuid,
    assigned_user_id uuid references public.users (id),
    scheduled_start timestamptz not null,
    scheduled_end timestamptz not null,
    status text not null default 'scheduled',
    started_at timestamptz,
    completed_at timestamptz,
    completed_by uuid references public.users (id),
    sync_status text not null default 'uploaded',
    is_demo boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint job_status_known check (status in ('scheduled', 'in_progress', 'completed', 'cancelled')),
    constraint sync_status_known check (sync_status in ('pending', 'uploading', 'uploaded', 'failed', 'retrying')),
    constraint job_window check (scheduled_end > scheduled_start),
    constraint job_completion_pair check (
        (
            status = 'completed'
            and started_at is not null
            and completed_at is not null
            and completed_by is not null
        )
        or (
            status = 'in_progress'
            and started_at is not null
            and completed_at is null
            and completed_by is null
        )
        or (
            status = 'scheduled'
            and started_at is null
            and completed_at is null
            and completed_by is null
        )
        or (
            status = 'cancelled'
            and completed_at is null
            and completed_by is null
        )
    ),
    constraint service_jobs_organization_id_id_key unique (organization_id, id),
    constraint job_version_key unique (organization_id, id, contract_version_id),
    constraint service_jobs_location_id_fkey
        foreign key (organization_id, location_id) references public.locations (organization_id, id),
    constraint service_jobs_client_id_fkey
        foreign key (organization_id, client_id) references public.clients (organization_id, id),
    constraint service_jobs_contract_id_fkey
        foreign key (organization_id, contract_id) references public.contracts (organization_id, id),
    constraint service_jobs_contract_version_id_fkey
        foreign key (organization_id, contract_id, contract_version_id)
        references public.contract_versions (organization_id, contract_id, id),
    constraint service_jobs_schedule_id_fkey
        foreign key (organization_id, schedule_id) references public.service_schedules (organization_id, id)
);

create unique index one_job_per_occurrence
    on public.service_jobs (organization_id, schedule_id, scheduled_start)
    where schedule_id is not null;

create index service_jobs_org_assignee_start_idx
    on public.service_jobs (organization_id, assigned_user_id, scheduled_start);

create index service_jobs_organization_id_location_id_scheduled_start_idx
    on public.service_jobs (organization_id, location_id, scheduled_start);

create index service_jobs_organization_id_status_idx
    on public.service_jobs (organization_id, status);

create index service_jobs_organization_id_contract_id_idx
    on public.service_jobs (organization_id, contract_id);

create index service_jobs_completed_by_idx on public.service_jobs (completed_by);

create table public.service_job_requirements (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    service_job_id uuid not null,
    contract_requirement_id uuid not null,
    requirement_text text not null,
    requires_photo boolean not null,
    is_mandatory boolean not null,
    sort_order integer not null,
    status text not null default 'missing',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint job_requirement_text_present check (char_length(requirement_text) > 0),
    constraint job_requirement_sort_nonnegative check (sort_order >= 0),
    constraint job_requirement_status_known check (status in ('missing', 'satisfied', 'exception')),
    constraint service_job_requirements_organization_id_id_key unique (organization_id, id),
    constraint job_requirement_key unique (organization_id, service_job_id, id),
    constraint requirement_once_per_job unique (service_job_id, contract_requirement_id),
    constraint service_job_requirements_job_id_fkey
        foreign key (organization_id, service_job_id) references public.service_jobs (organization_id, id),
    constraint service_job_requirements_requirement_id_fkey
        foreign key (organization_id, contract_requirement_id)
        references public.contract_requirements (organization_id, id)
);

create index service_job_requirements_organization_id_service_job_id_idx
    on public.service_job_requirements (organization_id, service_job_id);

create index service_job_requirements_organization_id_status_idx
    on public.service_job_requirements (organization_id, status);

create table public.evidence_records (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    service_job_id uuid not null,
    service_job_requirement_id uuid not null,
    captured_by uuid not null references public.users (id),
    captured_at timestamptz not null,
    received_at timestamptz,
    latitude double precision,
    longitude double precision,
    horizontal_accuracy_meters double precision,
    sync_status text not null,
    is_demo boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint sync_status_known check (sync_status in ('pending', 'uploading', 'uploaded', 'failed', 'retrying')),
    constraint evidence_coordinates check (
        (latitude is null and longitude is null)
        or (latitude between -90 and 90 and longitude between -180 and 180)
    ),
    constraint evidence_accuracy check (
        horizontal_accuracy_meters is null
        or (
            horizontal_accuracy_meters >= 0
            and latitude is not null
            and longitude is not null
        )
    ),
    constraint evidence_received check (
        (sync_status = 'uploaded' and received_at is not null)
        or (sync_status <> 'uploaded' and received_at is null)
    ),
    constraint evidence_records_organization_id_id_key unique (organization_id, id),
    constraint one_evidence_per_requirement unique (organization_id, service_job_requirement_id),
    constraint evidence_requirement_key unique (organization_id, service_job_requirement_id, id),
    constraint evidence_records_job_requirement_fkey
        foreign key (organization_id, service_job_id, service_job_requirement_id)
        references public.service_job_requirements (organization_id, service_job_id, id)
);

create index evidence_records_organization_id_service_job_id_idx
    on public.evidence_records (organization_id, service_job_id);

create index evidence_records_organization_id_captured_by_captured_at_idx
    on public.evidence_records (organization_id, captured_by, captured_at);

create table public.evidence_files (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    evidence_record_id uuid not null,
    bucket text not null default 'evidence',
    object_path text not null,
    mime_type text not null,
    byte_size bigint not null,
    sha256 text not null,
    sync_status text not null,
    uploaded_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint evidence_bucket_known check (bucket = 'evidence'),
    constraint evidence_path_present check (char_length(object_path) > 0),
    constraint evidence_mime_present check (char_length(mime_type) > 0),
    constraint evidence_byte_size_positive check (byte_size > 0),
    constraint evidence_sha256_hex check (sha256 ~ '^[0-9a-f]{64}$'),
    constraint sync_status_known check (sync_status in ('pending', 'uploading', 'uploaded', 'failed', 'retrying')),
    constraint evidence_file_uploaded check (
        (sync_status = 'uploaded' and uploaded_at is not null)
        or (sync_status <> 'uploaded' and uploaded_at is null)
    ),
    constraint evidence_files_organization_id_id_key unique (organization_id, id),
    constraint one_file_per_evidence unique (evidence_record_id),
    constraint evidence_object_once unique (bucket, object_path),
    constraint evidence_files_evidence_record_id_fkey
        foreign key (organization_id, evidence_record_id)
        references public.evidence_records (organization_id, id)
);

create index evidence_files_organization_id_sync_status_idx
    on public.evidence_files (organization_id, sync_status);

create table public.exceptions (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    service_job_id uuid not null,
    service_job_requirement_id uuid not null,
    recorded_by uuid not null references public.users (id),
    recorded_at timestamptz not null,
    reason text not null,
    sync_status text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint exception_reason_present check (char_length(reason) > 0),
    constraint sync_status_known check (sync_status in ('pending', 'uploading', 'uploaded', 'failed', 'retrying')),
    constraint exceptions_organization_id_id_key unique (organization_id, id),
    constraint one_exception_per_requirement unique (organization_id, service_job_requirement_id),
    constraint exception_requirement_key unique (organization_id, service_job_requirement_id, id),
    constraint exceptions_job_requirement_fkey
        foreign key (organization_id, service_job_id, service_job_requirement_id)
        references public.service_job_requirements (organization_id, service_job_id, id)
);

create index exceptions_organization_id_service_job_id_idx
    on public.exceptions (organization_id, service_job_id);

create index exceptions_recorded_by_idx on public.exceptions (recorded_by);

create table public.disputes (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    client_id uuid not null,
    location_id uuid not null,
    service_date date not null,
    complaint text not null,
    recorded_by uuid not null references public.users (id),
    status text not null default 'open',
    sync_status text not null,
    is_demo boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint dispute_complaint_present check (char_length(complaint) > 0),
    constraint dispute_status_known check (status in ('open', 'closed')),
    constraint sync_status_known check (sync_status in ('pending', 'uploading', 'uploaded', 'failed', 'retrying')),
    constraint disputes_organization_id_id_key unique (organization_id, id),
    constraint disputes_client_id_fkey
        foreign key (organization_id, client_id) references public.clients (organization_id, id),
    constraint disputes_location_id_fkey
        foreign key (organization_id, location_id) references public.locations (organization_id, id)
);

create index disputes_organization_id_status_service_date_idx
    on public.disputes (organization_id, status, service_date);

create index disputes_organization_id_client_id_idx on public.disputes (organization_id, client_id);
create index disputes_organization_id_location_id_idx on public.disputes (organization_id, location_id);
create index disputes_recorded_by_idx on public.disputes (recorded_by);

create table public.dispute_items (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    dispute_id uuid not null,
    service_job_id uuid not null,
    service_job_requirement_id uuid not null,
    evidence_record_id uuid,
    exception_id uuid,
    outcome text not null,
    created_at timestamptz not null default now(),
    constraint dispute_outcome_known check (outcome in ('satisfied', 'missing', 'exception')),
    constraint dispute_outcome_links check (
        (outcome = 'satisfied' and evidence_record_id is not null and exception_id is null)
        or (outcome = 'exception' and exception_id is not null and evidence_record_id is null)
        or (outcome = 'missing' and evidence_record_id is null and exception_id is null)
    ),
    constraint dispute_items_organization_id_id_key unique (organization_id, id),
    constraint requirement_once_per_dispute unique (dispute_id, service_job_requirement_id),
    constraint dispute_items_dispute_id_fkey
        foreign key (organization_id, dispute_id) references public.disputes (organization_id, id),
    constraint dispute_items_job_requirement_fkey
        foreign key (organization_id, service_job_id, service_job_requirement_id)
        references public.service_job_requirements (organization_id, service_job_id, id),
    constraint dispute_items_evidence_record_id_fkey
        foreign key (organization_id, service_job_requirement_id, evidence_record_id)
        references public.evidence_records (organization_id, service_job_requirement_id, id),
    constraint dispute_items_exception_id_fkey
        foreign key (organization_id, service_job_requirement_id, exception_id)
        references public.exceptions (organization_id, service_job_requirement_id, id)
);

create index dispute_items_organization_id_dispute_id_idx
    on public.dispute_items (organization_id, dispute_id);

create index dispute_items_organization_id_service_job_id_idx
    on public.dispute_items (organization_id, service_job_id);

create table public.reports (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null,
    dispute_id uuid not null,
    status text not null default 'generating',
    bucket text not null default 'reports',
    object_path text,
    generated_by uuid references public.users (id),
    generated_at timestamptz,
    failure_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint report_status_known check (status in ('generating', 'ready', 'failed')),
    constraint report_bucket_known check (bucket = 'reports'),
    constraint report_ready_file check (
        (
            status = 'ready'
            and object_path is not null
            and char_length(object_path) > 0
            and generated_at is not null
            and generated_by is not null
            and failure_reason is null
        )
        or (
            status = 'failed'
            and failure_reason is not null
            and char_length(failure_reason) > 0
            and object_path is null
        )
        or (
            status = 'generating'
            and object_path is null
            and generated_at is null
            and failure_reason is null
        )
    ),
    constraint reports_organization_id_id_key unique (organization_id, id),
    constraint one_report_per_dispute unique (dispute_id),
    constraint reports_dispute_id_fkey
        foreign key (organization_id, dispute_id) references public.disputes (organization_id, id)
);

create index reports_organization_id_status_idx on public.reports (organization_id, status);
create index reports_generated_by_idx on public.reports (generated_by);

create table public.audit_logs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations (id),
    actor_user_id uuid references public.users (id),
    action text not null,
    entity_type text not null,
    entity_id uuid not null,
    metadata jsonb not null default '{}',
    created_at timestamptz not null default now(),
    constraint audit_action_present check (char_length(action) > 0),
    constraint audit_entity_type_present check (char_length(entity_type) > 0),
    constraint audit_logs_organization_id_id_key unique (organization_id, id)
);

create index audit_logs_organization_id_created_at_idx
    on public.audit_logs (organization_id, created_at desc);

create index audit_logs_entity_type_entity_id_idx
    on public.audit_logs (entity_type, entity_id);

create index audit_logs_actor_user_id_idx on public.audit_logs (actor_user_id);

create function private.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create function private.current_actor()
returns uuid
language plpgsql
stable
as $$
declare
    actor uuid;
begin
    if to_regprocedure('auth.uid()') is null then
        return null;
    end if;
    execute 'select auth.uid()' into actor;
    return actor;
exception
    when others then
        return null;
end;
$$;

create function private.member_organization(subject uuid)
returns uuid
language sql
stable
as $$
    select organization_id
    from public.organization_members
    where user_id = subject;
$$;

create function private.reject_cross_tenant_user()
returns trigger
language plpgsql
as $$
declare
    subject uuid;
    member_org uuid;
    row_org uuid;
begin
    if tg_argv[0] = 'created_by' then
        subject := new.created_by;
    elsif tg_argv[0] = 'approved_by' then
        subject := new.approved_by;
    elsif tg_argv[0] = 'assigned_user_id' then
        subject := new.assigned_user_id;
    elsif tg_argv[0] = 'completed_by' then
        subject := new.completed_by;
    elsif tg_argv[0] = 'captured_by' then
        subject := new.captured_by;
    elsif tg_argv[0] = 'recorded_by' then
        subject := new.recorded_by;
    elsif tg_argv[0] = 'generated_by' then
        subject := new.generated_by;
    elsif tg_argv[0] = 'actor_user_id' then
        subject := new.actor_user_id;
    end if;
    if subject is null then
        return new;
    end if;
    member_org := private.member_organization(subject);
    if tg_table_name = 'organizations' then
        row_org := new.id;
    else
        row_org := new.organization_id;
    end if;
    if member_org is not null and member_org is distinct from row_org then
        raise exception 'user is not a member of this organization';
    end if;
    return new;
end;
$$;

create function private.member_locations_in_org()
returns trigger
language plpgsql
as $$
declare
    location_id uuid;
begin
    if new.role <> 'manager' then
        return new;
    end if;
    foreach location_id in array new.location_ids loop
        if not exists (
            select 1
            from public.locations
            where locations.organization_id = new.organization_id
              and locations.id = location_id
        ) then
            raise exception 'manager location is not in this organization';
        end if;
    end loop;
    return new;
end;
$$;

create function private.seed_free_subscription()
returns trigger
language plpgsql
as $$
begin
    insert into public.subscriptions (organization_id, plan, status, source)
    values (new.id, 'free', 'active', 'seed');
    return new;
end;
$$;

create function private.on_contract_version_write()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'INSERT' then
        select is_demo
        into new.is_demo
        from public.contracts
        where organization_id = new.organization_id
          and id = new.contract_id;
    end if;

    if tg_op = 'UPDATE' and old.status = 'superseded' then
        raise exception 'superseded contract version cannot be changed';
    end if;

    if tg_op = 'UPDATE' and old.status = 'approved' then
        if new.status is distinct from 'superseded' then
            raise exception 'approved contract version can only become superseded';
        end if;
        if new.organization_id is distinct from old.organization_id
            or new.contract_id is distinct from old.contract_id
            or new.version_number is distinct from old.version_number
            or new.bucket is distinct from old.bucket
            or new.object_path is distinct from old.object_path
            or new.extraction is distinct from old.extraction
            or new.approved_at is distinct from old.approved_at
            or new.approved_by is distinct from old.approved_by
            or new.is_demo is distinct from old.is_demo
            or new.created_at is distinct from old.created_at
            or new.id is distinct from old.id
        then
            raise exception 'approved contract version content cannot be changed';
        end if;
        return new;
    end if;

    if new.status = 'approved' and (tg_op = 'INSERT' or old.status is distinct from 'approved') then
        update public.contract_versions
        set status = 'superseded'
        where organization_id = new.organization_id
          and contract_id = new.contract_id
          and status = 'approved'
          and id is distinct from new.id;
    end if;

    return new;
end;
$$;

create function private.on_contract_version_approved()
returns trigger
language plpgsql
as $$
begin
    if new.status = 'approved' and (tg_op = 'INSERT' or old.status is distinct from 'approved') then
        update public.contracts
        set current_version_id = new.id,
            status = 'active'
        where organization_id = new.organization_id
          and id = new.contract_id;
    end if;
    return new;
end;
$$;

create function private.freeze_approved_requirements()
returns trigger
language plpgsql
as $$
declare
    version_status text;
    version_id uuid;
begin
    version_id := coalesce(new.contract_version_id, old.contract_version_id);
    select status
    into version_status
    from public.contract_versions
    where id = version_id;
    if version_status in ('approved', 'superseded') then
        raise exception 'requirements on an approved contract version cannot be changed';
    end if;
    if tg_op = 'DELETE' then
        return old;
    end if;
    return new;
end;
$$;

create function private.schedule_matches_contract_location()
returns trigger
language plpgsql
as $$
declare
    contract_location uuid;
begin
    select location_id
    into contract_location
    from public.contracts
    where organization_id = new.organization_id
      and id = new.contract_id;
    if contract_location is distinct from new.location_id then
        raise exception 'schedule location must match the contract location';
    end if;
    return new;
end;
$$;

create function private.on_service_job_write()
returns trigger
language plpgsql
as $$
declare
    contract_client uuid;
    contract_location uuid;
    contract_demo boolean;
    version_status text;
begin
    if tg_op = 'INSERT' then
        select client_id, location_id, is_demo
        into contract_client, contract_location, contract_demo
        from public.contracts
        where organization_id = new.organization_id
          and id = new.contract_id;
        if contract_client is distinct from new.client_id
            or contract_location is distinct from new.location_id
        then
            raise exception 'job client and location must match the contract';
        end if;
        select status
        into version_status
        from public.contract_versions
        where organization_id = new.organization_id
          and id = new.contract_version_id;
        if version_status is distinct from 'approved' then
            raise exception 'job must pin an approved contract version';
        end if;
        new.is_demo := contract_demo;
    end if;
    return new;
end;
$$;

create function private.on_service_job_requirement_write()
returns trigger
language plpgsql
as $$
declare
    job_version uuid;
    requirement_version uuid;
    uploaded_file boolean;
    uploaded_exception boolean;
begin
    select contract_version_id
    into job_version
    from public.service_jobs
    where organization_id = new.organization_id
      and id = new.service_job_id;
    select contract_version_id
    into requirement_version
    from public.contract_requirements
    where organization_id = new.organization_id
      and id = new.contract_requirement_id;
    if job_version is distinct from requirement_version then
        raise exception 'job requirement must belong to the job contract version';
    end if;

    if tg_op = 'UPDATE' then
        if new.requirement_text is distinct from old.requirement_text
            or new.requires_photo is distinct from old.requires_photo
            or new.is_mandatory is distinct from old.is_mandatory
            or new.sort_order is distinct from old.sort_order
            or new.contract_requirement_id is distinct from old.contract_requirement_id
            or new.service_job_id is distinct from old.service_job_id
            or new.organization_id is distinct from old.organization_id
            or new.id is distinct from old.id
            or new.created_at is distinct from old.created_at
        then
            raise exception 'job requirement snapshot cannot be changed';
        end if;
    end if;

    select exists (
        select 1
        from public.evidence_records
        join public.evidence_files
          on evidence_files.evidence_record_id = evidence_records.id
         and evidence_files.organization_id = evidence_records.organization_id
        where evidence_records.organization_id = new.organization_id
          and evidence_records.service_job_requirement_id = new.id
          and evidence_files.sync_status = 'uploaded'
    ) into uploaded_file;
    select exists (
        select 1
        from public.exceptions
        where organization_id = new.organization_id
          and service_job_requirement_id = new.id
          and sync_status = 'uploaded'
    ) into uploaded_exception;

    if new.status = 'satisfied' and not uploaded_file then
        raise exception 'satisfied requires an uploaded evidence file';
    end if;
    if new.status = 'exception' and not uploaded_exception then
        raise exception 'exception status requires an uploaded exception';
    end if;
    return new;
end;
$$;

create function private.on_evidence_record_write()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'INSERT' then
        select is_demo
        into new.is_demo
        from public.service_jobs
        where organization_id = new.organization_id
          and id = new.service_job_id;
        return new;
    end if;
    if new.captured_by is distinct from old.captured_by
        or new.captured_at is distinct from old.captured_at
        or new.latitude is distinct from old.latitude
        or new.longitude is distinct from old.longitude
        or new.horizontal_accuracy_meters is distinct from old.horizontal_accuracy_meters
        or new.is_demo is distinct from old.is_demo
        or new.organization_id is distinct from old.organization_id
        or new.service_job_id is distinct from old.service_job_id
        or new.service_job_requirement_id is distinct from old.service_job_requirement_id
        or new.created_at is distinct from old.created_at
        or new.id is distinct from old.id
    then
        raise exception 'evidence provenance cannot be changed';
    end if;
    return new;
end;
$$;

create function private.freeze_evidence_file()
returns trigger
language plpgsql
as $$
begin
    if new.bucket is distinct from old.bucket
        or new.object_path is distinct from old.object_path
        or new.mime_type is distinct from old.mime_type
        or new.byte_size is distinct from old.byte_size
        or new.sha256 is distinct from old.sha256
        or new.evidence_record_id is distinct from old.evidence_record_id
        or new.organization_id is distinct from old.organization_id
        or new.created_at is distinct from old.created_at
        or new.id is distinct from old.id
    then
        raise exception 'evidence file content cannot be changed';
    end if;
    return new;
end;
$$;

create function private.freeze_exception()
returns trigger
language plpgsql
as $$
begin
    if new.recorded_by is distinct from old.recorded_by
        or new.recorded_at is distinct from old.recorded_at
        or new.reason is distinct from old.reason
        or new.service_job_id is distinct from old.service_job_id
        or new.service_job_requirement_id is distinct from old.service_job_requirement_id
        or new.organization_id is distinct from old.organization_id
        or new.created_at is distinct from old.created_at
        or new.id is distinct from old.id
    then
        raise exception 'exception content cannot be changed';
    end if;
    return new;
end;
$$;

create function private.freeze_dispute()
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

create function private.dispute_item_is_uploaded()
returns trigger
language plpgsql
as $$
begin
    if tg_op <> 'INSERT' then
        raise exception 'dispute items are insert-only';
    end if;
    if new.outcome = 'satisfied' and not exists (
        select 1
        from public.evidence_files
        where organization_id = new.organization_id
          and evidence_record_id = new.evidence_record_id
          and sync_status = 'uploaded'
    ) then
        raise exception 'satisfied dispute item requires an uploaded evidence file';
    end if;
    if new.outcome = 'exception' and not exists (
        select 1
        from public.exceptions
        where organization_id = new.organization_id
          and id = new.exception_id
          and sync_status = 'uploaded'
    ) then
        raise exception 'exception dispute item requires an uploaded exception';
    end if;
    return new;
end;
$$;

create function private.freeze_report()
returns trigger
language plpgsql
as $$
begin
    if new.dispute_id is distinct from old.dispute_id
        or new.organization_id is distinct from old.organization_id
        or new.id is distinct from old.id
        or new.created_at is distinct from old.created_at
    then
        raise exception 'report identity cannot be changed';
    end if;
    return new;
end;
$$;

create function private.audit_event()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare
    row_org uuid;
    row_id uuid;
    audit_action text;
    audit_entity text;
    audit_metadata jsonb := '{}'::jsonb;
begin
    if tg_op = 'DELETE' then
        row_org := old.organization_id;
        row_id := old.id;
    else
        row_org := case
            when tg_table_name = 'organizations' then new.id
            else new.organization_id
        end;
        row_id := new.id;
    end if;

    if tg_table_name = 'organization_members' then
        audit_action := 'membership.changed';
        audit_entity := 'organization_member';
    elsif tg_table_name = 'contract_versions' then
        if new.status is distinct from 'approved' or (tg_op = 'UPDATE' and old.status = 'approved') then
            return coalesce(new, old);
        end if;
        audit_action := 'contract_version.approved';
        audit_entity := 'contract_version';
    elsif tg_table_name = 'service_jobs' then
        if new.status is distinct from 'completed' or (tg_op = 'UPDATE' and old.status = 'completed') then
            return new;
        end if;
        audit_action := 'service_job.completed';
        audit_entity := 'service_job';
    elsif tg_table_name = 'evidence_files' then
        if new.sync_status is distinct from 'uploaded' or (tg_op = 'UPDATE' and old.sync_status = 'uploaded') then
            return new;
        end if;
        audit_action := 'evidence.received';
        audit_entity := 'evidence_file';
    elsif tg_table_name = 'exceptions' then
        if new.sync_status is distinct from 'uploaded' or (tg_op = 'UPDATE' and old.sync_status = 'uploaded') then
            return new;
        end if;
        audit_action := 'exception.received';
        audit_entity := 'exception';
    elsif tg_table_name = 'disputes' then
        if tg_op = 'INSERT' then
            audit_action := 'dispute.opened';
        elsif new.status = 'closed' and old.status is distinct from 'closed' then
            audit_action := 'dispute.closed';
        else
            return new;
        end if;
        audit_entity := 'dispute';
    elsif tg_table_name = 'reports' then
        if new.status is distinct from 'ready' or (tg_op = 'UPDATE' and old.status = 'ready') then
            return new;
        end if;
        audit_action := 'report.ready';
        audit_entity := 'report';
    elsif tg_table_name = 'subscriptions' then
        if tg_op <> 'UPDATE' then
            return coalesce(new, old);
        end if;
        audit_action := 'subscription.changed';
        audit_entity := 'subscription';
        audit_metadata := jsonb_build_object('last_event_id', new.last_event_id);
    else
        return coalesce(new, old);
    end if;

    insert into public.audit_logs (organization_id, actor_user_id, action, entity_type, entity_id, metadata)
    values (row_org, private.current_actor(), audit_action, audit_entity, row_id, audit_metadata);
    return coalesce(new, old);
end;
$$;

create trigger organizations_touch_updated_at before update on public.organizations
    for each row execute function private.touch_updated_at();
create trigger users_touch_updated_at before update on public.users
    for each row execute function private.touch_updated_at();
create trigger organization_members_touch_updated_at before update on public.organization_members
    for each row execute function private.touch_updated_at();
create trigger clients_touch_updated_at before update on public.clients
    for each row execute function private.touch_updated_at();
create trigger locations_touch_updated_at before update on public.locations
    for each row execute function private.touch_updated_at();
create trigger subscriptions_touch_updated_at before update on public.subscriptions
    for each row execute function private.touch_updated_at();
create trigger contracts_touch_updated_at before update on public.contracts
    for each row execute function private.touch_updated_at();
create trigger service_schedules_touch_updated_at before update on public.service_schedules
    for each row execute function private.touch_updated_at();
create trigger service_jobs_touch_updated_at before update on public.service_jobs
    for each row execute function private.touch_updated_at();
create trigger service_job_requirements_touch_updated_at before update on public.service_job_requirements
    for each row execute function private.touch_updated_at();
create trigger evidence_records_touch_updated_at before update on public.evidence_records
    for each row execute function private.touch_updated_at();
create trigger evidence_files_touch_updated_at before update on public.evidence_files
    for each row execute function private.touch_updated_at();
create trigger exceptions_touch_updated_at before update on public.exceptions
    for each row execute function private.touch_updated_at();
create trigger disputes_touch_updated_at before update on public.disputes
    for each row execute function private.touch_updated_at();
create trigger reports_touch_updated_at before update on public.reports
    for each row execute function private.touch_updated_at();

create trigger organizations_reject_cross_tenant_user before insert or update on public.organizations
    for each row execute function private.reject_cross_tenant_user('created_by');
create trigger contract_versions_reject_cross_tenant_user before insert or update on public.contract_versions
    for each row execute function private.reject_cross_tenant_user('approved_by');
create trigger service_jobs_reject_assigned_user before insert or update on public.service_jobs
    for each row execute function private.reject_cross_tenant_user('assigned_user_id');
create trigger service_jobs_reject_completed_by before insert or update on public.service_jobs
    for each row execute function private.reject_cross_tenant_user('completed_by');
create trigger evidence_records_reject_cross_tenant_user before insert or update on public.evidence_records
    for each row execute function private.reject_cross_tenant_user('captured_by');
create trigger exceptions_reject_cross_tenant_user before insert or update on public.exceptions
    for each row execute function private.reject_cross_tenant_user('recorded_by');
create trigger disputes_reject_cross_tenant_user before insert or update on public.disputes
    for each row execute function private.reject_cross_tenant_user('recorded_by');
create trigger reports_reject_cross_tenant_user before insert or update on public.reports
    for each row execute function private.reject_cross_tenant_user('generated_by');
create trigger audit_logs_reject_cross_tenant_user before insert or update on public.audit_logs
    for each row execute function private.reject_cross_tenant_user('actor_user_id');

create trigger organization_members_locations_in_org before insert or update on public.organization_members
    for each row execute function private.member_locations_in_org();

create trigger organizations_seed_free_subscription after insert on public.organizations
    for each row execute function private.seed_free_subscription();

create trigger contract_versions_write before insert or update on public.contract_versions
    for each row execute function private.on_contract_version_write();
create trigger contract_versions_approved after insert or update on public.contract_versions
    for each row execute function private.on_contract_version_approved();
create trigger contract_requirements_freeze before insert or update or delete on public.contract_requirements
    for each row execute function private.freeze_approved_requirements();
create trigger service_schedules_match_location before insert or update on public.service_schedules
    for each row execute function private.schedule_matches_contract_location();
create trigger service_jobs_write before insert or update on public.service_jobs
    for each row execute function private.on_service_job_write();
create trigger service_job_requirements_write before insert or update on public.service_job_requirements
    for each row execute function private.on_service_job_requirement_write();
create trigger evidence_records_write before insert or update on public.evidence_records
    for each row execute function private.on_evidence_record_write();
create trigger evidence_files_freeze before update on public.evidence_files
    for each row execute function private.freeze_evidence_file();
create trigger exceptions_freeze before update on public.exceptions
    for each row execute function private.freeze_exception();
create trigger disputes_freeze before update on public.disputes
    for each row execute function private.freeze_dispute();
create trigger dispute_items_uploaded before insert or update or delete on public.dispute_items
    for each row execute function private.dispute_item_is_uploaded();
create trigger reports_freeze before update on public.reports
    for each row execute function private.freeze_report();

create trigger organization_members_audit after insert or update or delete on public.organization_members
    for each row execute function private.audit_event();
create trigger contract_versions_audit after insert or update on public.contract_versions
    for each row execute function private.audit_event();
create trigger service_jobs_audit after insert or update on public.service_jobs
    for each row execute function private.audit_event();
create trigger evidence_files_audit after insert or update on public.evidence_files
    for each row execute function private.audit_event();
create trigger exceptions_audit after insert or update on public.exceptions
    for each row execute function private.audit_event();
create trigger disputes_audit after insert or update on public.disputes
    for each row execute function private.audit_event();
create trigger reports_audit after insert or update on public.reports
    for each row execute function private.audit_event();
create trigger subscriptions_audit after update on public.subscriptions
    for each row execute function private.audit_event();
