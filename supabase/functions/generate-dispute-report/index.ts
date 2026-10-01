import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { requireReportReaderAuth } from "./auth.ts";
import { loadReportContext } from "./load.ts";
import { buildReportPdf } from "./pdf.ts";
import {
  createServiceClient,
  getExistingReport,
  markFailed,
  markGenerating,
  markReady,
  reportObjectPath,
  signedReportUrl,
  uploadReportPdf,
} from "./persist.ts";
import type { GenerateReportRequest, ReportRow } from "./types.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return json({ ok: false, error: { message: "Only POST is supported." } }, 405);
  }
  const auth = await requireReportReaderAuth(req);
  if (auth instanceof Response) {
    return auth;
  }

  let body: GenerateReportRequest;
  try {
    body = await req.json();
  } catch {
    return json({ ok: false, error: { message: "Request body must be JSON." } }, 400);
  }
  const disputeId = body.dispute_id?.trim();
  if (!disputeId) {
    return json({ ok: false, error: { message: "dispute_id is required." } }, 400);
  }
  const force = body.force === true;

  const admin = createServiceClient();
  const existing = await getExistingReport(admin, auth.organizationId, disputeId);
  if (existing?.status === "ready" && existing.object_path && !force) {
    const signedUrl = await signedReportUrl(admin, existing.object_path);
    return json({ ok: true, report: existing, signed_url: signedUrl });
  }

  const reportId = existing?.id ?? crypto.randomUUID();
  try {
    await markGenerating(admin, auth.organizationId, disputeId, reportId);

    const context = await loadReportContext(auth.userClient, auth.organizationId, disputeId);
    if (context instanceof Response) {
      await markFailed(admin, auth.organizationId, disputeId, "Could not load dispute context.");
      return context;
    }

    const pdfBytes = await buildReportPdf(context);
    const objectPath = reportObjectPath(auth.organizationId, disputeId, reportId);
    await uploadReportPdf(admin, objectPath, pdfBytes);
    const report = await markReady(admin, auth.organizationId, disputeId, auth.userId, objectPath);
    const signedUrl = await signedReportUrl(admin, objectPath);
    return json({ ok: true, report, signed_url: signedUrl });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Report generation failed.";
    await markFailed(admin, auth.organizationId, disputeId, message);
    return json({ ok: false, error: { message } }, 500);
  }
});

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
