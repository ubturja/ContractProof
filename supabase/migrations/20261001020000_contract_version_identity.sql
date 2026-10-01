-- Version identity: who uploaded it, when it takes effect, freeze those fields after approval.

alter table public.contract_versions
    add column created_by uuid references public.users (id),
    add column effective_on date;

update public.contract_versions as version
set created_by = coalesce(
        version.approved_by,
        (
            select organization.created_by
            from public.contracts as contract
            join public.organizations as organization
                on organization.id = contract.organization_id
            where contract.id = version.contract_id
              and contract.organization_id = version.organization_id
        )
    ),
    effective_on = coalesce(version.effective_on, version.created_at::date)
where version.created_by is null
   or version.effective_on is null;

alter table public.contract_versions
    alter column created_by set not null,
    alter column effective_on set not null;

create index contract_versions_created_by_idx on public.contract_versions (created_by);

create trigger contract_versions_reject_created_by
    before insert or update on public.contract_versions
    for each row execute function private.reject_cross_tenant_user('created_by');

create or replace function private.on_contract_version_write()
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
            or new.created_by is distinct from old.created_by
            or new.effective_on is distinct from old.effective_on
            or new.original_file_name is distinct from old.original_file_name
            or new.mime_type is distinct from old.mime_type
            or new.byte_size is distinct from old.byte_size
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
