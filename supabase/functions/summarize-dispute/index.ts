import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { requireDisputeReaderAuth } from "./auth.ts";
import { loadDisputeContext } from "./load.ts";
import { summarizeWithLlm } from "./llm.ts";
import { assertValidSummary } from "./validate.ts";
import type { SummarizeDisputeRequest } from "./types.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return json({ ok: false, error: { message: "Only POST is supported." } }, 405);
  }
  const auth = await requireDisputeReaderAuth(req);
  if (auth instanceof Response) {
    return auth;
  }

  let body: SummarizeDisputeRequest;
  try {
    body = await req.json();
  } catch {
    return json({ ok: false, error: { message: "Request body must be JSON." } }, 400);
  }
  const disputeId = body.dispute_id?.trim();
  if (!disputeId) {
    return json({ ok: false, error: { message: "dispute_id is required." } }, 400);
  }

  const context = await loadDisputeContext(auth.userClient, auth.organizationId, disputeId);
  if (context instanceof Response) {
    return context;
  }

  const llm = await summarizeWithLlm(context);
  if (!llm.ok) {
    return json({ ok: false, error: { message: llm.message } }, 502);
  }

  let summary;
  try {
    summary = assertValidSummary(llm.payload);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Summary validation failed.";
    return json({ ok: false, error: { message } }, 502);
  }

  const generatedAt = new Date().toISOString();
  const { error: updateError } = await auth.userClient
    .from("disputes")
    .update({
      ai_summary_json: summary,
      ai_summary_generated_at: generatedAt,
    })
    .eq("id", disputeId)
    .eq("organization_id", auth.organizationId);
  if (updateError) {
    return json({ ok: false, error: { message: "Could not save summary." } }, 500);
  }

  return json({ ok: true, summary });
});

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
