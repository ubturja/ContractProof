alter table public.service_jobs
    drop constraint if exists job_status_known;

alter table public.service_jobs
    add constraint job_status_known check (
        status in (
            'scheduled',
            'in_progress',
            'completed',
            'cancelled',
            'incomplete',
            'disputed'
        )
    );

alter table public.service_jobs
    drop constraint if exists job_completion_pair;

alter table public.service_jobs
    add constraint job_completion_pair check (
        (
            status = 'completed'
            and started_at is not null
            and completed_at is not null
            and completed_by is not null
        )
        or (
            status = 'disputed'
            and started_at is not null
            and completed_at is not null
            and completed_by is not null
        )
        or (
            status = 'in_progress'
            and started_at is not null
            and completed_at is null
            and completed_by is null
        )
        or (
            status = 'incomplete'
            and started_at is not null
            and completed_at is null
            and completed_by is null
        )
        or (
            status = 'scheduled'
            and started_at is null
            and completed_at is null
            and completed_by is null
        )
        or (
            status = 'cancelled'
            and completed_at is null
            and completed_by is null
        )
    );
