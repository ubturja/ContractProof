-- A new approved version must not retarget jobs or freeze-break old requirements.
-- Run inside a transaction and roll back.

begin;

insert into auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
    confirmation_token, recovery_token, email_change_token_new, email_change
)
select
    coalesce(
        (select id from auth.instances limit 1),
        '00000000-0000-0000-0000-000000000000'
    ),
    '21111111-1111-4111-8111-111111111111'::uuid,
    'authenticated',
    'authenticated',
    'version-pin-owner@example.com',
    crypt('password', gen_salt('bf')),
    now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{}'::jsonb,
    now(),
    now(),
    '',
    '',
    '',
    ''
where not exists (
    select 1 from auth.users where id = '21111111-1111-4111-8111-111111111111'
);

insert into public.users (id, email, display_name) values
    ('21111111-1111-4111-8111-111111111111', 'version-pin-owner@example.com', 'Pin Owner')
on conflict (id) do nothing;

insert into public.organizations (id, name, created_by) values
    (
        '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        'Pin Org',
        '21111111-1111-4111-8111-111111111111'
    );

insert into public.organization_members (organization_id, user_id, role, client_id, location_ids) values
    (
        '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        '21111111-1111-4111-8111-111111111111',
        'owner',
        null,
        '{}'
    );

insert into public.clients (id, organization_id, name) values
    (
        '21111111-cccc-4ccc-8ccc-ccccccccccc1',
        '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        'Pin Client'
    );

insert into public.locations (id, organization_id, client_id, name, timezone) values
    (
        '21111111-dddd-4ddd-8ddd-ddddddddddd1',
        '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        '21111111-cccc-4ccc-8ccc-ccccccccccc1',
        'Pin Lobby',
        'America/New_York'
    );

insert into public.contracts (id, organization_id, client_id, location_id, title, starts_on) values
    (
        '21111111-eeee-4eee-8eee-eeeeeeeeeee1',
        '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        '21111111-cccc-4ccc-8ccc-ccccccccccc1',
        '21111111-dddd-4ddd-8ddd-ddddddddddd1',
        'Pin Contract',
        date '2026-01-01'
    );

insert into public.contract_versions (
    id, organization_id, contract_id, version_number, status, created_by, effective_on
) values (
    '21111111-ffff-4fff-8fff-fffffffffff1',
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
    '21111111-eeee-4eee-8eee-eeeeeeeeeee1',
    1,
    'uploaded',
    '21111111-1111-4111-8111-111111111111',
    date '2026-01-01'
);

insert into public.contract_requirements (
    id, organization_id, contract_version_id, sort_order, requirement_text, requires_photo, is_mandatory, source
) values (
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa2',
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
    '21111111-ffff-4fff-8fff-fffffffffff1',
    0,
    'Sweep lobby',
    true,
    true,
    'manual'
);

update public.contract_versions
set status = 'approved',
    approved_at = now(),
    approved_by = '21111111-1111-4111-8111-111111111111'
where id = '21111111-ffff-4fff-8fff-fffffffffff1';

insert into public.service_jobs (
    id, organization_id, location_id, client_id, contract_id, contract_version_id,
    assigned_user_id, scheduled_start, scheduled_end
) values (
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
    '21111111-dddd-4ddd-8ddd-ddddddddddd1',
    '21111111-cccc-4ccc-8ccc-ccccccccccc1',
    '21111111-eeee-4eee-8eee-eeeeeeeeeee1',
    '21111111-ffff-4fff-8fff-fffffffffff1',
    '21111111-1111-4111-8111-111111111111',
    timestamptz '2026-09-30 13:00+00',
    timestamptz '2026-09-30 14:00+00'
);

insert into public.contract_versions (
    id, organization_id, contract_id, version_number, status, created_by, effective_on
) values (
    '21111111-ffff-4fff-8fff-fffffffffff2',
    '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
    '21111111-eeee-4eee-8eee-eeeeeeeeeee1',
    2,
    'uploaded',
    '21111111-1111-4111-8111-111111111111',
    date '2026-02-01'
);

update public.contract_versions
set status = 'approved',
    approved_at = now(),
    approved_by = '21111111-1111-4111-8111-111111111111'
where id = '21111111-ffff-4fff-8fff-fffffffffff2';

do $$
declare
    job_version uuid;
    req_version uuid;
    req_text text;
    v1_status text;
    v2_status text;
    current_id uuid;
    contract_status text;
begin
    select contract_version_id into job_version
    from public.service_jobs
    where id = '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa3';
    select contract_version_id, requirement_text into req_version, req_text
    from public.contract_requirements
    where id = '21111111-aaaa-4aaa-8aaa-aaaaaaaaaaa2';
    select status into v1_status
    from public.contract_versions
    where id = '21111111-ffff-4fff-8fff-fffffffffff1';
    select status into v2_status
    from public.contract_versions
    where id = '21111111-ffff-4fff-8fff-fffffffffff2';
    select current_version_id, status into current_id, contract_status
    from public.contracts
    where id = '21111111-eeee-4eee-8eee-eeeeeeeeeee1';
    if job_version is distinct from '21111111-ffff-4fff-8fff-fffffffffff1' then
        raise exception 'job moved to version %', job_version;
    end if;
    if req_version is distinct from '21111111-ffff-4fff-8fff-fffffffffff1' then
        raise exception 'requirement moved to version %', req_version;
    end if;
    if req_text is distinct from 'Sweep lobby' then
        raise exception 'requirement text changed to %', req_text;
    end if;
    if v1_status is distinct from 'superseded' then
        raise exception 'version 1 status is %', v1_status;
    end if;
    if v2_status is distinct from 'approved' then
        raise exception 'version 2 status is %', v2_status;
    end if;
    if current_id is distinct from '21111111-ffff-4fff-8fff-fffffffffff2' then
        raise exception 'current version is %', current_id;
    end if;
    if contract_status is distinct from 'active' then
        raise exception 'contract status is %', contract_status;
    end if;
end
$$;

rollback;
