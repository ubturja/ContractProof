create table public.push_device_tokens (
    user_id uuid not null references public.users (id) on delete cascade,
    token text not null,
    platform text not null default 'android',
    updated_at timestamptz not null default now(),
    primary key (user_id, token),
    constraint push_token_platform_known check (platform in ('android', 'ios'))
);

alter table public.push_device_tokens enable row level security;
alter table public.push_device_tokens force row level security;

create policy push_device_tokens_select_own on public.push_device_tokens
for select to authenticated
using (user_id = auth.uid());

create policy push_device_tokens_insert_own on public.push_device_tokens
for insert to authenticated
with check (user_id = auth.uid());

create policy push_device_tokens_update_own on public.push_device_tokens
for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

create policy push_device_tokens_delete_own on public.push_device_tokens
for delete to authenticated
using (user_id = auth.uid());
