alter table public.service_schedules
    add column frequency text not null default 'weekly';

alter table public.service_schedules
    add constraint schedule_frequency_known
        check (frequency in ('daily', 'weekdays', 'weekly'));

comment on column public.service_schedules.frequency is
    'Recurrence: daily, weekdays (Mon-Fri), or weekly on weekday.';
