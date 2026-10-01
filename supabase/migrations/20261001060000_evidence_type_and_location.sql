alter table public.evidence_records
    add column evidence_type text not null default 'photo',
    add column location_id uuid;

alter table public.evidence_records
    add constraint evidence_type_known check (
        evidence_type in ('photo', 'checklist_completion', 'timestamp')
    );

alter table public.evidence_records
    add constraint evidence_records_location_id_fkey
        foreign key (organization_id, location_id)
        references public.locations (organization_id, id);

create index evidence_records_organization_id_location_id_idx
    on public.evidence_records (organization_id, location_id)
    where location_id is not null;
