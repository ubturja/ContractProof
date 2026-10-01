import type { SupabaseClient } from "@supabase/supabase-js";
import type { DisputeContext } from "./types.ts";

export async function loadDisputeContext(
  client: SupabaseClient,
  organizationId: string,
  disputeId: string,
): Promise<DisputeContext | Response> {
  const { data: dispute, error: disputeError } = await client
    .from("disputes")
    .select(
      "id, organization_id, complaint, service_date, service_job_id, disputed_service_job_requirement_id",
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

  const { data: job } = await client
    .from("service_jobs")
    .select("id, contract_id, contract_version_id, started_at, completed_at, assigned_user_id")
    .eq("id", dispute.service_job_id)
    .eq("organization_id", organizationId)
    .maybeSingle();
  if (!job) {
    return notFound("Service job not found.");
  }

  const { data: contract } = await client
    .from("contracts")
    .select("title")
    .eq("id", job.contract_id)
    .eq("organization_id", organizationId)
    .maybeSingle();

  const { data: version } = await client
    .from("contract_versions")
    .select("version_number")
    .eq("id", job.contract_version_id)
    .eq("organization_id", organizationId)
    .maybeSingle();

  const { data: requirements } = await client
    .from("service_job_requirements")
    .select("id, requirement_text")
    .eq("service_job_id", dispute.service_job_id)
    .eq("organization_id", organizationId);

  const reqTextById = new Map(
    (requirements ?? []).map((r) => [r.id as string, r.requirement_text as string]),
  );

  const disputedText =
    reqTextById.get(dispute.disputed_service_job_requirement_id as string) ??
    "Not recorded";

  const { data: items } = await client
    .from("dispute_items")
    .select("service_job_requirement_id, outcome, evidence_record_id, exception_id")
    .eq("dispute_id", disputeId)
    .eq("organization_id", organizationId);

  const evidenceIds = (items ?? [])
    .map((i) => i.evidence_record_id)
    .filter((id): id is string => typeof id === "string");
  const exceptionIds = (items ?? [])
    .map((i) => i.exception_id)
    .filter((id): id is string => typeof id === "string");

  const evidenceById = new Map<string, { captured_at: string; evidence_type: string }>();
  if (evidenceIds.length > 0) {
    const { data: evidenceRows } = await client
      .from("evidence_records")
      .select("id, captured_at, evidence_type")
      .eq("organization_id", organizationId)
      .in("id", evidenceIds);
    for (const row of evidenceRows ?? []) {
      evidenceById.set(row.id as string, {
        captured_at: row.captured_at as string,
        evidence_type: row.evidence_type as string,
      });
    }
  }

  const exceptionById = new Map<string, string>();
  if (exceptionIds.length > 0) {
    const { data: exceptionRows } = await client
      .from("exceptions")
      .select("id, reason")
      .eq("organization_id", organizationId)
      .in("id", exceptionIds);
    for (const row of exceptionRows ?? []) {
      exceptionById.set(row.id as string, row.reason as string);
    }
  }

  let workerName: string | null = null;
  if (job.assigned_user_id) {
    const { data: userRow } = await client
      .from("users")
      .select("display_name")
      .eq("id", job.assigned_user_id)
      .maybeSingle();
    workerName = (userRow?.display_name as string) ?? null;
  }

  const mappedItems = (items ?? []).map((item) => {
    const reqId = item.service_job_requirement_id as string;
    const evidence = item.evidence_record_id
      ? evidenceById.get(item.evidence_record_id as string)
      : null;
    const exceptionReason = item.exception_id
      ? exceptionById.get(item.exception_id as string) ?? null
      : null;
    return {
      requirementText: reqTextById.get(reqId) ?? reqId,
      outcome: item.outcome as string,
      evidenceCapturedAt: evidence?.captured_at ?? null,
      evidenceType: evidence?.evidence_type ?? null,
      exceptionReason,
    };
  });

  return {
    disputeId,
    organizationId,
    complaint: dispute.complaint as string,
    serviceDate: dispute.service_date as string,
    disputedRequirementText: disputedText,
    contractTitle: (contract?.title as string) ?? "Not recorded",
    versionLabel: version?.version_number != null
      ? `v${version.version_number}`
      : "Not recorded",
    jobStartedAt: (job.started_at as string) ?? null,
    jobCompletedAt: (job.completed_at as string) ?? null,
    workerName,
    items: mappedItems,
  };
}

function notFound(message: string): Response {
  return new Response(
    JSON.stringify({ ok: false, error: { message } }),
    { status: 404, headers: { "Content-Type": "application/json" } },
  );
}
