import type { SupabaseClient } from "@supabase/supabase-js";
import type { ReportPdfContext } from "./types.ts";
import { createServiceClient } from "./admin.ts";

export async function loadReportContext(
  userClient: SupabaseClient,
  organizationId: string,
  disputeId: string,
): Promise<ReportPdfContext | Response> {
  const { data: dispute, error: disputeError } = await userClient
    .from("disputes")
    .select(
      "id, organization_id, complaint, service_date, service_job_id, disputed_service_job_requirement_id, client_id, location_id, created_at, complaint_attachment_object_path, complaint_attachment_mime_type",
    )
    .eq("id", disputeId)
    .eq("organization_id", organizationId)
    .maybeSingle();
  if (disputeError || !dispute) {
    return notFound("Dispute not found.");
  }
  if (!dispute.service_job_id) {
    return notFound("Dispute is missing service job linkage.");
  }

  const { data: clientRow } = await userClient
    .from("clients")
    .select("name")
    .eq("id", dispute.client_id)
    .eq("organization_id", organizationId)
    .maybeSingle();
  const { data: locationRow } = await userClient
    .from("locations")
    .select("name")
    .eq("id", dispute.location_id)
    .eq("organization_id", organizationId)
    .maybeSingle();

  const { data: job } = await userClient
    .from("service_jobs")
    .select(
      "id, contract_id, contract_version_id, started_at, completed_at, assigned_user_id, scheduled_start, scheduled_end, service_date",
    )
    .eq("id", dispute.service_job_id)
    .eq("organization_id", organizationId)
    .maybeSingle();
  if (!job) {
    return notFound("Service job not found.");
  }

  const { data: contract } = await userClient
    .from("contracts")
    .select("title")
    .eq("id", job.contract_id)
    .eq("organization_id", organizationId)
    .maybeSingle();

  const { data: version } = await userClient
    .from("contract_versions")
    .select("version_number")
    .eq("id", job.contract_version_id)
    .eq("organization_id", organizationId)
    .maybeSingle();

  const { data: requirements } = await userClient
    .from("service_job_requirements")
    .select("id, requirement_text, sort_order")
    .eq("service_job_id", dispute.service_job_id)
    .eq("organization_id", organizationId)
    .order("sort_order");

  const reqTextById = new Map(
    (requirements ?? []).map((r) => [r.id as string, r.requirement_text as string]),
  );
  const disputedId = dispute.disputed_service_job_requirement_id as string | null;

  const { data: items } = await userClient
    .from("dispute_items")
    .select("service_job_requirement_id, outcome")
    .eq("dispute_id", disputeId)
    .eq("organization_id", organizationId);

  const { data: evidenceRows } = await userClient
    .from("evidence_records")
    .select("id, service_job_requirement_id, captured_at, evidence_type, sync_status")
    .eq("service_job_id", dispute.service_job_id)
    .eq("organization_id", organizationId)
    .eq("sync_status", "uploaded");

  const evidenceIds = (evidenceRows ?? []).map((r) => r.id as string);
  const filesByRecord = new Map<string, { object_path: string; mime_type: string }>();
  if (evidenceIds.length > 0) {
    const { data: files } = await userClient
      .from("evidence_files")
      .select("evidence_record_id, object_path, mime_type, sync_status")
      .eq("organization_id", organizationId)
      .in("evidence_record_id", evidenceIds)
      .eq("sync_status", "uploaded");
    for (const file of files ?? []) {
      filesByRecord.set(file.evidence_record_id as string, {
        object_path: file.object_path as string,
        mime_type: file.mime_type as string,
      });
    }
  }

  const admin = createServiceClient();
  const evidence = await Promise.all(
    (evidenceRows ?? []).map(async (row) => {
      const file = filesByRecord.get(row.id as string);
      let bytes: Uint8Array | null = null;
      if (file && row.evidence_type === "photo") {
        const { data, error } = await admin.storage.from("evidence").download(file.object_path);
        if (!error && data) {
          bytes = new Uint8Array(await data.arrayBuffer());
        }
      }
      return {
        requirementText: reqTextById.get(row.service_job_requirement_id as string) ??
          (row.service_job_requirement_id as string),
        type: row.evidence_type as string,
        capturedAt: row.captured_at as string,
        objectPath: file?.object_path ?? null,
        mimeType: file?.mime_type ?? null,
        bytes,
      };
    }),
  );

  const { data: exceptionRows } = await userClient
    .from("exceptions")
    .select("service_job_requirement_id, reason, recorded_at, sync_status")
    .eq("service_job_id", dispute.service_job_id)
    .eq("organization_id", organizationId)
    .eq("sync_status", "uploaded");

  let workerName = "Not recorded";
  if (job.assigned_user_id) {
    const { data: userRow } = await userClient
      .from("users")
      .select("display_name")
      .eq("id", job.assigned_user_id)
      .maybeSingle();
    if (userRow?.display_name) {
      workerName = userRow.display_name as string;
    }
  }

  const timeline = buildTimeline(job, dispute, evidence, exceptionRows ?? [], reqTextById);

  const acknowledgements: Array<{ label: string; at: string; detail: string }> = [];
  for (const ev of evidence) {
    if (ev.type === "checklist_completion") {
      acknowledgements.push({
        label: "Checklist recorded",
        at: ev.capturedAt,
        detail: ev.requirementText,
      });
    }
    if (ev.type === "timestamp") {
      acknowledgements.push({
        label: "Timestamp recorded",
        at: ev.capturedAt,
        detail: ev.requirementText,
      });
    }
  }
  acknowledgements.push({
    label: "Dispute filed",
    at: dispute.created_at as string,
    detail: dispute.complaint as string,
  });

  const attachments: Array<{ label: string; path: string; mimeType: string | null }> = [];
  if (dispute.complaint_attachment_object_path) {
    attachments.push({
      label: "Complaint attachment",
      path: dispute.complaint_attachment_object_path as string,
      mimeType: (dispute.complaint_attachment_mime_type as string) ?? null,
    });
  }
  for (const ev of evidence) {
    if (ev.objectPath) {
      attachments.push({
        label: "Evidence file",
        path: ev.objectPath,
        mimeType: ev.mimeType,
      });
    }
  }

  return {
    organizationId,
    disputeId,
    header: {
      clientName: (clientRow?.name as string) ?? "Not recorded",
      locationName: (locationRow?.name as string) ?? "Not recorded",
      serviceDate: dispute.service_date as string,
      complaint: dispute.complaint as string,
      disputedRequirement: disputedId
        ? (reqTextById.get(disputedId) ?? "Not recorded")
        : "Not recorded",
      contractTitle: (contract?.title as string) ?? "Not recorded",
      versionLabel: version?.version_number != null
        ? `v${version.version_number}`
        : "Not recorded",
    },
    contractRequirements: (requirements ?? []).map((r) => ({
      text: r.requirement_text as string,
      disputed: r.id === disputedId,
    })),
    scheduledService: {
      start: job.scheduled_start as string,
      end: job.scheduled_end as string,
      serviceDate: job.service_date as string,
    },
    assignedPersonnel: workerName,
    timeline,
    evidence,
    exceptions: (exceptionRows ?? []).map((ex) => ({
      requirementText: reqTextById.get(ex.service_job_requirement_id as string) ??
        (ex.service_job_requirement_id as string),
      reason: ex.reason as string,
      recordedAt: ex.recorded_at as string,
    })),
    acknowledgements,
    coverage: (items ?? []).map((item) => ({
      requirementText: reqTextById.get(item.service_job_requirement_id as string) ??
        (item.service_job_requirement_id as string),
      outcome: item.outcome as string,
    })),
    attachments,
  };
}

function buildTimeline(
  job: Record<string, unknown>,
  dispute: Record<string, unknown>,
  evidence: Array<{ capturedAt: string; requirementText: string; type: string }>,
  exceptions: Array<{ recorded_at: string; service_job_requirement_id: string; reason: string }>,
  reqTextById: Map<string, string>,
): Array<{ at: string; title: string; body: string }> {
  const events: Array<{ at: string; title: string; body: string; order: number }> = [];
  events.push({
    at: job.scheduled_start as string,
    title: "Job scheduled",
    body: `Scheduled ${job.scheduled_start} – ${job.scheduled_end}`,
    order: 0,
  });
  if (job.started_at) {
    events.push({
      at: job.started_at as string,
      title: "Job started",
      body: "Start recorded.",
      order: 1,
    });
  }
  if (job.completed_at) {
    events.push({
      at: job.completed_at as string,
      title: "Job completed",
      body: "Completion recorded.",
      order: 1,
    });
  }
  for (const ev of evidence) {
    events.push({
      at: ev.capturedAt,
      title: `Evidence recorded (${ev.type})`,
      body: ev.requirementText,
      order: 2,
    });
  }
  for (const ex of exceptions) {
    events.push({
      at: ex.recorded_at as string,
      title: "Exception recorded",
      body: `${reqTextById.get(ex.service_job_requirement_id as string) ?? ""}: ${ex.reason}`,
      order: 3,
    });
  }
  events.push({
    at: dispute.created_at as string,
    title: "Dispute filed",
    body: dispute.complaint as string,
    order: 4,
  });
  return events
    .sort((a, b) => a.at.localeCompare(b.at) || a.order - b.order)
    .map(({ at, title, body }) => ({ at, title, body }));
}

function notFound(message: string): Response {
  return new Response(
    JSON.stringify({ ok: false, error: { message } }),
    { status: 404, headers: { "Content-Type": "application/json" } },
  );
}
