-- Locations belong to one client in the same organization.
-- identifier_kind and identifier_value are reserved for later QR/NFC. The app does not write them.

alter table public.locations
    add column client_id uuid not null,
    add column zone_code text,
    add column identifier_kind text,
    add column identifier_value text;

alter table public.locations
    add constraint locations_client_id_fkey
        foreign key (organization_id, client_id) references public.clients (organization_id, id);

alter table public.locations
    add constraint location_identifier_kind_known
        check (identifier_kind is null or identifier_kind in ('qr', 'nfc'));

alter table public.locations
    add constraint location_identifier_pair
        check (
            (identifier_kind is null and identifier_value is null)
            or (
                identifier_kind is not null
                and identifier_value is not null
                and char_length(identifier_value) > 0
            )
        );

create index locations_organization_id_client_id_idx
    on public.locations (organization_id, client_id);
