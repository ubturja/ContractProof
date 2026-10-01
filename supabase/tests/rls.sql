-- Local tenant checks. Apply all three migrations first.
-- The session user is postgres. auth.uid() is the local stub, not Supabase's function.
-- Seed runs as the superuser. The five checks run as authenticated.

do $$
declare
    tables int;
    forced int;
begin
    select count(*) into tables
    from pg_class as relation
    join pg_namespace as namespace on namespace.oid = relation.relnamespace
    where namespace.nspname = 'public'
      and relation.relkind = 'r';
    select count(*) into forced
    from pg_class as relation
    join pg_namespace as namespace on namespace.oid = relation.relnamespace
    where namespace.nspname = 'public'
      and relation.relkind = 'r'
      and relation.relrowsecurity
      and relation.relforcerowsecurity;
    if tables <> 20 or forced <> 20 then
        raise exception 'expected 20 forced tables, found % tables and % forced', tables, forced;
    end if;
    if exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and roles && array['anon', 'public']::name[]
    ) then
        raise exception 'anon or public has a row-level security policy';
    end if;
end
$$;

insert into auth.users (id) values
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222'),
    ('33333333-3333-3333-3333-333333333333'),
    ('44444444-4444-4444-4444-444444444444'),
    ('55555555-5555-5555-5555-555555555555'),
    ('66666666-6666-6666-6666-666666666666');

insert into public.users (id, email, display_name) values
    ('11111111-1111-1111-1111-111111111111', 'owner-a@example.com', 'Owner A'),
    ('22222222-2222-2222-2222-222222222222', 'owner-b@example.com', 'Owner B'),
    ('33333333-3333-3333-3333-333333333333', 'manager-a@example.com', 'Manager A'),
    ('44444444-4444-4444-4444-444444444444', 'client-a@example.com', 'Client A'),
    ('55555555-5555-5555-5555-555555555555', 'cleaner-a@example.com', 'Cleaner A'),
    ('66666666-6666-6666-6666-666666666666', 'founder@example.com', 'Founder');

insert into public.organizations (id, name, created_by) values
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'Organization A', '11111111-1111-1111-1111-111111111111'),
    ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'Organization B', '22222222-2222-2222-2222-222222222222');

insert into public.clients (id, organization_id, name) values
    ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'Client A'),
    ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'Client B');

insert into public.locations (id, organization_id, client_id, name, timezone) values
    ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'Lobby A', 'America/New_York'),
    ('d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'Lobby B', 'America/Chicago');

insert into public.organization_members (organization_id, user_id, role, client_id, location_ids) values
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '11111111-1111-1111-1111-111111111111', 'owner', null, '{}'),
    ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '22222222-2222-2222-2222-222222222222', 'owner', null, '{}'),
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '33333333-3333-3333-3333-333333333333', 'manager', null, array['d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1']::uuid[]),
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '44444444-4444-4444-4444-444444444444', 'client', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', '{}'),
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '55555555-5555-5555-5555-555555555555', 'cleaner', null, '{}');

insert into public.contracts (id, organization_id, client_id, location_id, title, starts_on) values
    ('e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', 'Lobby A', date '2026-01-01'),
    ('e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e2e2', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', 'Lobby B', date '2026-01-01');

insert into public.contract_versions (
    id, organization_id, contract_id, version_number, status, approved_at, approved_by, created_by, effective_on
) values
    ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1', 1, 'approved', now(), '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', date '2026-01-01'),
    ('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e2e2', 1, 'approved', now(), '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', date '2026-01-01');

insert into public.service_jobs (
    id, organization_id, location_id, client_id, contract_id, contract_version_id,
    assigned_user_id, scheduled_start, scheduled_end
) values
    (
        'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1',
        'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1',
        'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',
        'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
        '55555555-5555-5555-5555-555555555555',
        timestamptz '2026-09-30 13:00+00',
        timestamptz '2026-09-30 14:00+00'
    ),
    (
        'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1',
        'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1',
        'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1',
        'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
        '11111111-1111-1111-1111-111111111111',
        timestamptz '2026-09-30 15:00+00',
        timestamptz '2026-09-30 16:00+00'
    ),
    (
        'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3',
        'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0',
        'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2',
        'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2',
        'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e2e2',
        'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2',
        '22222222-2222-2222-2222-222222222222',
        timestamptz '2026-09-30 13:00+00',
        timestamptz '2026-09-30 14:00+00'
    );

update public.service_jobs
set
    status = 'completed',
    started_at = timestamptz '2026-09-30 12:00+00',
    completed_at = timestamptz '2026-09-30 14:00+00',
    completed_by = '55555555-5555-5555-5555-555555555555'
where id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1';

insert into public.disputes (
    id, organization_id, client_id, location_id, service_date, complaint, recorded_by, sync_status
) values (
    'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4',
    'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
    'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1',
    'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1',
    date '2026-09-30',
    'Missed lobby',
    '11111111-1111-1111-1111-111111111111',
    'pending'
);

-- Check 1 and 2. Owner A.
select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', false);
set role authenticated;

do $$
declare
    own_clients int;
    other_clients int;
begin
    if current_user <> 'authenticated' then
        raise exception 'check 1 ran as %', current_user;
    end if;
    select count(*) into other_clients
    from public.clients
    where organization_id = 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0';
    select count(*) into own_clients
    from public.clients
    where organization_id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0';
    if other_clients <> 0 then
        raise exception 'check 1: owner A saw % clients from organization B', other_clients;
    end if;
    if own_clients <> 1 then
        raise exception 'check 1: owner A saw % of their own clients', own_clients;
    end if;
end
$$;

do $$
begin
    insert into public.clients (organization_id, name)
    values ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'Cross tenant');
    raise exception 'check 2: owner A inserted a client into organization B';
exception
    when insufficient_privilege then
        if sqlerrm not like '%row-level security%' then
            raise exception 'check 2: expected row-level security, got %', sqlerrm;
        end if;
end
$$;

reset role;

-- Check 3. Manager A cannot become owner.
select set_config('request.jwt.claim.sub', '33333333-3333-3333-3333-333333333333', false);
set role authenticated;

do $$
declare
    updated int;
begin
    if current_user <> 'authenticated' then
        raise exception 'check 3 ran as %', current_user;
    end if;
    update public.organization_members
    set role = 'owner'
    where user_id = '33333333-3333-3333-3333-333333333333';
    get diagnostics updated = row_count;
    if updated <> 0 then
        raise exception 'check 3: manager updated % membership rows', updated;
    end if;
exception
    when insufficient_privilege then
        if sqlerrm not like '%row-level security%' then
            raise exception 'check 3: expected row-level security, got %', sqlerrm;
        end if;
end
$$;

reset role;

do $$
declare
    stored text;
begin
    select role into stored
    from public.organization_members
    where user_id = '33333333-3333-3333-3333-333333333333';
    if stored is distinct from 'manager' then
        raise exception 'check 3: role is %', stored;
    end if;
end
$$;

-- Check 4. Client A.
select set_config('request.jwt.claim.sub', '44444444-4444-4444-4444-444444444444', false);
set role authenticated;

do $$
declare
    contract_count int;
    subscription_count int;
    dispute_count int;
    job_count int;
    user_count int;
    member_count int;
begin
    if current_user <> 'authenticated' then
        raise exception 'check 4 ran as %', current_user;
    end if;
    select count(*) into contract_count from public.contracts;
    select count(*) into subscription_count from public.subscriptions;
    select count(*) into dispute_count from public.disputes;
    select count(*) into job_count from public.service_jobs;
    select count(*) into user_count from public.users;
    select count(*) into member_count from public.organization_members;
    if contract_count <> 0 or subscription_count <> 0 then
        raise exception
            'check 4: client saw contracts %, subscriptions %',
            contract_count, subscription_count;
    end if;
    if dispute_count <> 1 or job_count <> 1 then
        raise exception
            'check 4: client saw % disputes and % completed jobs (expected 1 and 1)',
            dispute_count, job_count;
    end if;
    if user_count <> 1 or member_count <> 1 then
        raise exception 'check 4: client saw % users and % memberships', user_count, member_count;
    end if;
end
$$;

reset role;

-- Check 5. Cleaner A.
select set_config('request.jwt.claim.sub', '55555555-5555-5555-5555-555555555555', false);
set role authenticated;

do $$
declare
    other_org_jobs int;
    other_assignee_jobs int;
    own_jobs int;
begin
    if current_user <> 'authenticated' then
        raise exception 'check 5 ran as %', current_user;
    end if;
    select count(*) into other_org_jobs
    from public.service_jobs
    where organization_id = 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0';
    select count(*) into other_assignee_jobs
    from public.service_jobs
    where id = 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2';
    select count(*) into own_jobs
    from public.service_jobs
    where id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1';
    if other_org_jobs <> 0 then
        raise exception 'check 5: cleaner saw % jobs from organization B', other_org_jobs;
    end if;
    if other_assignee_jobs <> 0 then
        raise exception 'check 5: cleaner saw a job assigned to someone else';
    end if;
    if own_jobs <> 1 then
        raise exception 'check 5: cleaner saw % of their assigned job', own_jobs;
    end if;
end
$$;

reset role;

-- First organization. Founder has no membership yet.
select set_config('request.jwt.claim.sub', '66666666-6666-6666-6666-666666666666', false);
set role authenticated;

do $$
begin
    insert into public.organizations (id, name, created_by)
    values (
        'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9',
        'Northside',
        '66666666-6666-6666-6666-666666666666'
    );
end
$$;

do $$
begin
    insert into public.organization_members (organization_id, user_id, role)
    values (
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0',
        '66666666-6666-6666-6666-666666666666',
        'owner'
    );
    raise exception 'founder inserted an owner membership into organization A';
exception
    when insufficient_privilege then
        if sqlerrm not like '%row-level security%' then
            raise exception 'founder claim: expected row-level security, got %', sqlerrm;
        end if;
end
$$;

do $$
declare
    visible int;
begin
    insert into public.organization_members (organization_id, user_id, role)
    values (
        'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9',
        '66666666-6666-6666-6666-666666666666',
        'owner'
    );
    select count(*) into visible from public.organizations;
    if visible <> 1 then
        raise exception 'founder saw % organizations', visible;
    end if;
end
$$;

do $$
begin
    insert into public.organizations (name, created_by)
    values ('Second', '66666666-6666-6666-6666-666666666666');
    raise exception 'founder inserted a second organization';
exception
    when insufficient_privilege then
        if sqlerrm not like '%row-level security%' then
            raise exception 'second organization: expected row-level security, got %', sqlerrm;
        end if;
    when raise_exception then
        if sqlerrm = 'founder inserted a second organization'
            or sqlerrm not like '%not a member of this organization%'
        then
            raise;
        end if;
end
$$;

reset role;
