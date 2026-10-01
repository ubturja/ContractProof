-- ClearLine Facility Services — fictional hackathon demo data (is_demo = true).
-- UUID map: docs/development/demo-data.md
-- Auth users must exist in Supabase Auth with matching ids (see demo-mode.md).

-- Organization
insert into public.organizations (id, name, created_by) values
    ('11111111-1111-4111-8111-111111111101', 'ClearLine Facility Services', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01');

insert into public.users (id, email, display_name) values
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01', 'owner@clearline.demo', 'ClearLine Owner'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa02', 'manager@clearline.demo', 'ClearLine Manager'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03', 'cleaner@clearline.demo', 'ClearLine Cleaner'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa04', 'client@clearline.demo', 'Meridian Client Contact');

insert into public.organization_members (organization_id, user_id, role, client_id, location_ids) values
    ('11111111-1111-4111-8111-111111111101', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01', 'owner', null, '{}'),
    ('11111111-1111-4111-8111-111111111101', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa02', 'manager', null, '{}'),
    ('11111111-1111-4111-8111-111111111101', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03', 'cleaner', null, '{}'),
    (
        '11111111-1111-4111-8111-111111111101',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa04',
        'client',
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02',
        '{}'
    );

insert into public.clients (id, organization_id, name) values
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01', '11111111-1111-4111-8111-111111111101', 'Meridian Office Tower'),
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02', '11111111-1111-4111-8111-111111111101', 'Northstar Logistics'),
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb03', '11111111-1111-4111-8111-111111111101', 'Westbridge Medical Offices');

insert into public.locations (id, organization_id, client_id, name, timezone, status) values
    ('cccccccc-cccc-4ccc-8ccc-cccccccccc01', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01', 'Meridian Lobby', 'America/New_York', 'active'),
    ('cccccccc-cccc-4ccc-8ccc-cccccccccc02', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02', 'Northstar Dock Office', 'America/Chicago', 'active'),
    ('cccccccc-cccc-4ccc-8ccc-cccccccccc03', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb03', 'Westbridge Suite 200', 'America/Los_Angeles', 'active');

insert into public.contracts (id, organization_id, client_id, location_id, title, status, is_demo) values
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd01', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01', 'cccccccc-cccc-4ccc-8ccc-cccccccccc01', 'Meridian nightly clean', 'active', true),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd02', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02', 'cccccccc-cccc-4ccc-8ccc-cccccccccc02', 'Northstar weekly service', 'active', true),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd03', '11111111-1111-4111-8111-111111111101', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb03', 'cccccccc-cccc-4ccc-8ccc-cccccccccc03', 'Westbridge clinic clean', 'active', true);

insert into public.contract_versions (
    id, organization_id, contract_id, version_number, status, bucket, object_path,
    approved_at, approved_by, created_by, effective_on, is_demo
) values
    (
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01',
        '11111111-1111-4111-8111-111111111101',
        'dddddddd-dddd-4ddd-8ddd-dddddddddd01',
        1, 'extracted', 'contracts',
        '11111111-1111-4111-8111-111111111101/dddddddd-dddd-4ddd-8ddd-dddddddddd01/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01/contract.pdf',
        null, null, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01',
        current_date, true
    ),
    (
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02',
        '11111111-1111-4111-8111-111111111101',
        'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
        1, 'approved', 'contracts',
        '11111111-1111-4111-8111-111111111101/dddddddd-dddd-4ddd-8ddd-dddddddddd02/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02/contract.pdf',
        now(), 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01',
        current_date, true
    ),
    (
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03',
        '11111111-1111-4111-8111-111111111101',
        'dddddddd-dddd-4ddd-8ddd-dddddddddd03',
        1, 'approved', 'contracts',
        '11111111-1111-4111-8111-111111111101/dddddddd-dddd-4ddd-8ddd-dddddddddd03/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03/contract.pdf',
        now(), 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01',
        current_date, true
    );

update public.contracts set current_version_id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01'
where id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd01';
update public.contracts set current_version_id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02'
where id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd02';
update public.contracts set current_version_id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03'
where id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd03';

update public.contract_versions
set extraction = $$
{
  "schema_version": 1,
  "status": "completed",
  "extracted_at": "2026-10-01T14:00:00Z",
  "pipeline": {
    "text_engine": "unpdf",
    "llm_provider": "gemini",
    "llm_model": "demo-fixture",
    "used_fallback": false
  },
  "document": {
    "page_count": 2,
    "text_char_count": 1800,
    "ocr_page_indexes": [],
    "warnings": []
  },
  "visits": [
    {
      "weekday": 4,
      "start_time": "09:00",
      "end_time": "11:00",
      "timezone": "America/New_York",
      "starts_on": "2026-10-01",
      "ends_on": null,
      "confidence": 0.95
    }
  ],
  "requirements": [
    {
      "key": "req_mer_1",
      "task": "Empty trash receptacles",
      "requires_photo": true,
      "is_mandatory": true,
      "zone_code": null,
      "confidence": 0.93,
      "evidence_quote": "All lobby trash receptacles emptied each service night"
    },
    {
      "key": "req_mer_2",
      "task": "Vacuum main walkway",
      "requires_photo": true,
      "is_mandatory": true,
      "zone_code": null,
      "confidence": 0.91,
      "evidence_quote": "Main walkway vacuumed nightly"
    }
  ]
}
$$::jsonb
where id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01';

insert into public.contract_requirements (
    id, organization_id, contract_version_id, sort_order, requirement_text, requires_photo, is_mandatory, source
) values
    ('ffffffff-ffff-4fff-8fff-fffffffffff04', '11111111-1111-4111-8111-111111111101', 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02', 0, 'Sweep loading dock', true, true, 'manual'),
    ('ffffffff-ffff-4fff-8fff-fffffffffff05', '11111111-1111-4111-8111-111111111101', 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02', 1, 'Sanitize break room', true, true, 'manual');

-- Completed disputed job (client portal). Meridian Today job is created after owner approves ee01 (visit weekday 4).
insert into public.service_jobs (
    id, organization_id, location_id, client_id, contract_id, contract_version_id,
    assigned_user_id, scheduled_start, scheduled_end, status,
    started_at, completed_at, completed_by, is_demo
) values
    (
        '99999999-9999-4999-8999-999999999902',
        '11111111-1111-4111-8111-111111111101',
        'cccccccc-cccc-4ccc-8ccc-cccccccccc02',
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02',
        'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03',
        timestamptz '2026-10-01 08:00:00+00',
        timestamptz '2026-10-01 10:00:00+00',
        'disputed',
        timestamptz '2026-10-01 08:00:00+00',
        timestamptz '2026-10-01 10:00:00+00',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03',
        true
    );

insert into public.service_job_requirements (
    id, organization_id, service_job_id, contract_requirement_id,
    requirement_text, requires_photo, is_mandatory, sort_order, status
) values
    ('88888888-8888-4888-8888-888888888803', '11111111-1111-4111-8111-111111111101', '99999999-9999-4999-8999-999999999902', 'ffffffff-ffff-4fff-8fff-fffffffffff04', 'Sweep loading dock', true, true, 0, 'satisfied'),
    ('88888888-8888-4888-8888-888888888804', '11111111-1111-4111-8111-111111111101', '99999999-9999-4999-8999-999999999902', 'ffffffff-ffff-4fff-8fff-fffffffffff05', 'Sanitize break room', true, true, 1, 'exception');

insert into public.evidence_records (
    id, organization_id, service_job_id, service_job_requirement_id,
    captured_by, captured_at, received_at, sync_status, is_demo
) values
    (
        '77777777-7777-4777-8777-777777777701',
        '11111111-1111-4111-8111-111111111101',
        '99999999-9999-4999-8999-999999999902',
        '88888888-8888-4888-8888-888888888803',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03',
        timestamptz '2026-10-01 09:00:00+00',
        timestamptz '2026-10-01 09:00:00+00',
        'uploaded',
        true
    );

insert into public.evidence_files (
    id, organization_id, evidence_record_id, bucket, object_path, mime_type, byte_size, sha256, sync_status, uploaded_at
) values
    (
        '66666666-6666-4666-8666-666666666601',
        '11111111-1111-4111-8111-111111111101',
        '77777777-7777-4777-8777-777777777701',
        'evidence',
        '11111111-1111-4111-8111-111111111101/99999999-9999-4999-8999-999999999902/88888888-8888-4888-8888-888888888803/77777777-7777-4777-8777-777777777701',
        'image/jpeg',
        1024,
        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
        'uploaded',
        timestamptz '2026-10-01 09:00:00+00'
    );

insert into public.exceptions (
    id, organization_id, service_job_id, service_job_requirement_id,
    recorded_by, recorded_at, reason, sync_status
) values
    (
        '55555555-5555-4555-8555-555555555501',
        '11111111-1111-4111-8111-111111111101',
        '99999999-9999-4999-8999-999999999902',
        '88888888-8888-4888-8888-888888888804',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03',
        timestamptz '2026-10-01 09:30:00+00',
        'Area blocked — Dock gate locked',
        'uploaded'
    );

insert into public.disputes (
    id, organization_id, client_id, location_id, service_date, complaint,
    recorded_by, status, sync_status, is_demo, service_job_id, disputed_service_job_requirement_id
) values
    (
        '44444444-4444-4444-8444-444444444401',
        '11111111-1111-4111-8111-111111111101',
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb02',
        'cccccccc-cccc-4ccc-8ccc-cccccccccc02',
        date '2026-10-01',
        'Break room was not sanitized; only the dock photo was provided.',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa04',
        'open',
        'uploaded',
        true,
        '99999999-9999-4999-8999-999999999902',
        '88888888-8888-4888-8888-888888888804'
    );

update public.disputes
set
    ai_summary_json = $$
{
  "allegation": "Break room was not sanitized; only the dock photo was provided.",
  "requirements": [
    {
      "requirementText": "Sweep loading dock",
      "contractualContext": "Mandatory photo evidence for dock service."
    },
    {
      "requirementText": "Sanitize break room",
      "contractualContext": "Mandatory sanitization with photo or documented exception."
    }
  ],
  "recorded_evidence": ["Photo uploaded for Sweep loading dock on 2026-10-01."],
  "missing_evidence": [],
  "exceptions": ["Sanitize break room: Area blocked — Dock gate locked."],
  "neutral_overview": "The client alleges the break room was not sanitized. One dock photo is on file; break room work is recorded as an exception due to blocked access."
}
$$::jsonb,
    ai_summary_generated_at = timestamptz '2026-10-01 12:00:00+00'
where id = '44444444-4444-4444-8444-444444444401';

insert into public.dispute_items (
    id, organization_id, dispute_id, service_job_id, service_job_requirement_id,
    evidence_record_id, exception_id, outcome
) values
    (
        '33333333-3333-4333-8333-333333333301',
        '11111111-1111-4111-8111-111111111101',
        '44444444-4444-4444-8444-444444444401',
        '99999999-9999-4999-8999-999999999902',
        '88888888-8888-4888-8888-888888888803',
        '77777777-7777-4777-8777-777777777701',
        null,
        'satisfied'
    ),
    (
        '33333333-3333-4333-8333-333333333302',
        '11111111-1111-4111-8111-111111111101',
        '44444444-4444-4444-8444-444444444401',
        '99999999-9999-4999-8999-999999999902',
        '88888888-8888-4888-8888-888888888804',
        null,
        '55555555-5555-4555-8555-555555555501',
        'exception'
    );

insert into public.reports (
    id, organization_id, dispute_id, status, bucket, object_path,
    generated_by, generated_at
) values
    (
        '22222222-2222-4222-8222-222222222201',
        '11111111-1111-4111-8111-111111111101',
        '44444444-4444-4444-8444-444444444401',
        'ready',
        'reports',
        '11111111-1111-4111-8111-111111111101/44444444-4444-4444-8444-444444444401/evidence-report.pdf',
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01',
        timestamptz '2026-10-01 12:30:00+00'
    );
