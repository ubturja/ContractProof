create policy contract_requirements_delete on public.contract_requirements
for delete to authenticated
using (
    organization_id = (select membership.organization_id from private.current_membership() as membership)
    and (select membership.role from private.current_membership() as membership) = 'owner'
    and exists (
        select 1
        from public.contract_versions as version
        where version.organization_id = contract_requirements.organization_id
          and version.id = contract_requirements.contract_version_id
    )
);
