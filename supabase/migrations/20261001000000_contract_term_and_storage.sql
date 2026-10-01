-- Contract service term and private document bucket.
-- A contract location must belong to the same client. PDFs live in Storage, not on the contract row.

alter table public.contracts
    add column starts_on date not null,
    add column ends_on date;

alter table public.contracts
    add constraint contract_term_order
        check (ends_on is null or ends_on >= starts_on);

create function private.contract_matches_location_client()
returns trigger
language plpgsql
as $$
declare
    location_client uuid;
begin
    select client_id
    into location_client
    from public.locations
    where organization_id = new.organization_id
      and id = new.location_id;
    if location_client is distinct from new.client_id then
        raise exception 'contract location must belong to the contract client';
    end if;
    return new;
end;
$$;

create trigger contracts_match_location_client
    before insert or update on public.contracts
    for each row execute function private.contract_matches_location_client();

grant execute on function private.contract_matches_location_client() to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'contracts',
    'contracts',
    false,
    20971520,
    array['application/pdf']::text[]
)
on conflict (id) do nothing;

create policy contracts_objects_select on storage.objects
for select to authenticated
using (
    bucket_id = 'contracts'
    and (storage.foldername(name))[1] = (
        select membership.organization_id::text
        from private.current_membership() as membership
    )
    and (select membership.role from private.current_membership() as membership) in ('owner', 'manager')
);

create policy contracts_objects_insert on storage.objects
for insert to authenticated
with check (
    bucket_id = 'contracts'
    and (storage.foldername(name))[1] = (
        select membership.organization_id::text
        from private.current_membership() as membership
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);

create policy contracts_objects_update on storage.objects
for update to authenticated
using (
    bucket_id = 'contracts'
    and (storage.foldername(name))[1] = (
        select membership.organization_id::text
        from private.current_membership() as membership
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
)
with check (
    bucket_id = 'contracts'
    and (storage.foldername(name))[1] = (
        select membership.organization_id::text
        from private.current_membership() as membership
    )
    and (select membership.role from private.current_membership() as membership) = 'owner'
);
