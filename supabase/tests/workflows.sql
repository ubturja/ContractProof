-- Workflow RLS checks for requirements, evidence, exceptions, and disputes.
-- Run after migrations and tenant fixtures from rls.sql:
--   psql "$DATABASE_URL" -f supabase/tests/rls.sql -f supabase/tests/workflows.sql

insert into public.contract_requirements (
    id, organization_id, contract_version_id, sort_order, requirement_text, requires_photo, is_mandatory, source
) values
    (
        'f9f9f9f9-f9f9-f9f9-f9f9-f9f9f9f9f9f9',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
        0,
        'Vacuum lobby',
        true,
        true,
        'manual'
    ),
    (
        'f8f8f8f8-f8f8-f8f8-f8f8-f8f8f8f8f8f8',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
        1,
        'Mop entry',
        true,
        true,
        'manual'
    );

insert into public.service_job_requirements (
    id, organization_id, service_job_id, contract_requirement_id,
    requirement_text, requires_photo, is_mandatory, sort_order, status
) values
    (
        'b9b9b9b9-b9b9-b9b9-b9b9-b9b9b9b9b9b9',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
        'f9f9f9f9-f9f9-f9f9-f9f9-f9f9f9f9f9f9',
        'Vacuum lobby',
        true,
        true,
        0,
        'satisfied'
    ),
    (
        'b8b8b8b8-b8b8-b8b8-b8b8-b8b8b8b8b8b8',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
        'f8f8f8f8-f8f8-f8f8-f8f8-f8f8f8f8f8f8',
        'Mop entry',
        true,
        true,
        1,
        'exception'
    );

insert into public.evidence_records (
    id, organization_id, service_job_id, service_job_requirement_id,
    captured_by, captured_at, received_at, sync_status
) values
    (
        'c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
        'b9b9b9b9-b9b9-b9b9-b9b9-b9b9b9b9b9b9',
        '55555555-5555-5555-5555-555555555555',
        timestamptz '2026-09-30 13:30+00',
        timestamptz '2026-09-30 13:30+00',
        'uploaded'
    );

insert into public.evidence_files (
    id, organization_id, evidence_record_id, bucket, object_path, mime_type, byte_size, sha256, sync_status, uploaded_at
) values
    (
        'd9d9d9d9-d9d9-d9d9-d9d9-d9d9d9d9d9d9',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9',
        'evidence',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1/b9b9b9b9-b9b9-b9b9-b9b9-b9b9b9b9b9b9/c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9',
        'image/jpeg',
        512,
        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
        'uploaded',
        timestamptz '2026-09-30 13:31+00'
    );

insert into public.exceptions (
    id, organization_id, service_job_id, service_job_requirement_id,
    recorded_by, recorded_at, reason, sync_status
) values
    (
        'e9e9e9e9-e9e9-e9e9-e9e9-e9e9e9e9e9e9',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
        'b8b8b8b8-b8b8-b8b8-b8b8-b8b8b8b8b8b8',
        '55555555-5555-5555-5555-555555555555',
        timestamptz '2026-09-30 13:45+00',
        'Wet floor signage',
        'uploaded'
    );

update public.disputes
set
    service_job_id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
    disputed_service_job_requirement_id = 'b8b8b8b8-b8b8-b8b8-b8b8-b8b8b8b8b8b8'
where id = 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4';

-- Owner B must not read Org A workflow rows.
select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', false);
set role authenticated;

do $$
declare
    req_count int;
    evidence_count int;
    exception_count int;
    dispute_count int;
begin
    if current_user <> 'authenticated' then
        raise exception 'workflow check ran as %', current_user;
    end if;
    select count(*) into req_count
    from public.contract_requirements
    where organization_id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0';
    select count(*) into evidence_count
    from public.evidence_records
    where organization_id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0';
    select count(*) into exception_count
    from public.exceptions
    where organization_id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0';
    select count(*) into dispute_count
    from public.disputes
    where organization_id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0';
    if req_count <> 0 or evidence_count <> 0 or exception_count <> 0 or dispute_count <> 0 then
        raise exception
            'owner B saw org A workflow data: requirements %, evidence %, exceptions %, disputes %',
            req_count, evidence_count, exception_count, dispute_count;
    end if;
end
$$;

reset role;

-- Owner A sees its workflow rows.
select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', false);
set role authenticated;

do $$
declare
    evidence_count int;
begin
    select count(*) into evidence_count
    from public.evidence_records
    where id = 'c9c9c9c9-c9c9-c9c9-c9c9-c9c9c9c9c9c9';
    if evidence_count <> 1 then
        raise exception 'owner A expected 1 evidence row, saw %', evidence_count;
    end if;
end
$$;

reset role;
