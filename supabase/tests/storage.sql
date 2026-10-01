-- Cross-organization isolation for the private contracts bucket.
-- Run inside a transaction and roll back. Superuser seeds; checks run as authenticated.

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
    seed.id,
    'authenticated',
    'authenticated',
    seed.email,
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
from (
    values
        ('11111111-1111-1111-1111-111111111111'::uuid, 'storage-owner-a@example.com'),
        ('22222222-2222-2222-2222-222222222222'::uuid, 'storage-owner-b@example.com')
) as seed(id, email)
where not exists (select 1 from auth.users as existing where existing.id = seed.id);

insert into public.users (id, email, display_name) values
    ('11111111-1111-1111-1111-111111111111', 'storage-owner-a@example.com', 'Owner A'),
    ('22222222-2222-2222-2222-222222222222', 'storage-owner-b@example.com', 'Owner B')
on conflict (id) do nothing;

insert into public.organizations (id, name, created_by) values
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'Organization A', '11111111-1111-1111-1111-111111111111'),
    ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'Organization B', '22222222-2222-2222-2222-222222222222')
on conflict (id) do nothing;

insert into public.organization_members (organization_id, user_id, role, client_id, location_ids) values
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '11111111-1111-1111-1111-111111111111', 'owner', null, '{}'),
    ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '22222222-2222-2222-2222-222222222222', 'owner', null, '{}')
on conflict (user_id) do nothing;

insert into storage.objects (bucket_id, name)
values
    (
        'contracts',
        'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1/f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1.pdf'
    ),
    (
        'contracts',
        'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0/e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e2e2/f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2.pdf'
    );

select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', true);
set local role authenticated;

do $$
declare
    own_files int;
    other_files int;
begin
    if current_user <> 'authenticated' then
        raise exception 'storage check ran as %', current_user;
    end if;
    select count(*) into own_files
    from storage.objects
    where bucket_id = 'contracts'
      and name like 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/%';
    select count(*) into other_files
    from storage.objects
    where bucket_id = 'contracts'
      and name like 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0/%';
    if other_files <> 0 then
        raise exception 'owner A saw % contract files from organization B', other_files;
    end if;
    if own_files < 1 then
        raise exception 'owner A saw % of their own contract files', own_files;
    end if;
end
$$;

do $$
begin
    insert into storage.objects (bucket_id, name)
    values (
        'contracts',
        'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0/e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e2e2/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1.pdf'
    );
    raise exception 'owner A uploaded into organization B';
exception
    when insufficient_privilege then
        if sqlerrm not like '%row-level security%' then
            raise exception 'cross-org upload: expected row-level security, got %', sqlerrm;
        end if;
end
$$;

reset role;
rollback;
