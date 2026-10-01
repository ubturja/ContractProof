import type { SupabaseClient } from "@supabase/supabase-js";
import type { ReportRow } from "./types.ts";
import { createServiceClient } from "./admin.ts";

const REPORTS_BUCKET = "reports";

export async function getExistingReport(
  admin: SupabaseClient,
  organizationId: string,
  disputeId: string,
): Promise<ReportRow | null> {
  const { data } = await admin
    .from("reports")
    .select("*")
    .eq("dispute_id", disputeId)
    .eq("organization_id", organizationId)
    .maybeSingle();
  return (data as ReportRow) ?? null;
}

export async function markGenerating(
  admin: SupabaseClient,
  organizationId: string,
  disputeId: string,
  reportId: string,
): Promise<void> {
  const { data: existing } = await admin
    .from("reports")
    .select("id")
    .eq("dispute_id", disputeId)
    .maybeSingle();
  if (existing) {
    const { error } = await admin
      .from("reports")
      .update({
        status: "generating",
        object_path: null,
        generated_at: null,
        generated_by: null,
        failure_reason: null,
      })
      .eq("id", existing.id)
      .eq("organization_id", organizationId);
    if (error) throw error;
  } else {
    const { error } = await admin.from("reports").insert({
      id: reportId,
      organization_id: organizationId,
      dispute_id: disputeId,
      status: "generating",
      bucket: REPORTS_BUCKET,
    });
    if (error) throw error;
  }
}

export async function markReady(
  admin: SupabaseClient,
  organizationId: string,
  disputeId: string,
  userId: string,
  objectPath: string,
): Promise<ReportRow> {
  const generatedAt = new Date().toISOString();
  const { data, error } = await admin
    .from("reports")
    .update({
      status: "ready",
      object_path: objectPath,
      generated_by: userId,
      generated_at: generatedAt,
      failure_reason: null,
      bucket: REPORTS_BUCKET,
    })
    .eq("dispute_id", disputeId)
    .eq("organization_id", organizationId)
    .select("*")
    .maybeSingle();
  if (error || !data) {
    throw new Error("Could not mark report ready.");
  }
  return data as ReportRow;
}

export async function markFailed(
  admin: SupabaseClient,
  organizationId: string,
  disputeId: string,
  reason: string,
): Promise<void> {
  await admin
    .from("reports")
    .update({
      status: "failed",
      failure_reason: reason,
      object_path: null,
      generated_at: null,
      generated_by: null,
    })
    .eq("dispute_id", disputeId)
    .eq("organization_id", organizationId);
}

export async function uploadReportPdf(
  admin: SupabaseClient,
  objectPath: string,
  bytes: Uint8Array,
): Promise<void> {
  const { error } = await admin.storage.from(REPORTS_BUCKET).upload(objectPath, bytes, {
    contentType: "application/pdf",
    upsert: true,
  });
  if (error) {
    throw new Error(error.message);
  }
}

export async function signedReportUrl(
  admin: SupabaseClient,
  objectPath: string,
): Promise<string> {
  const { data, error } = await admin.storage
    .from(REPORTS_BUCKET)
    .createSignedUrl(objectPath, 900);
  if (error || !data?.signedUrl) {
    throw new Error("Could not sign report URL.");
  }
  return data.signedUrl;
}

export function reportObjectPath(
  organizationId: string,
  disputeId: string,
  reportId: string,
): string {
  return `${organizationId}/${disputeId}/${reportId}.pdf`;
}

export { createServiceClient };
