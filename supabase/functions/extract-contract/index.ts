import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { requireOwnerAuth } from "./auth.ts";
import { errorResponse } from "./errors.ts";
import { attachPipelineMetadata, extractWithLlm } from "./llm.ts";
import { downloadPdf, extractPdfText } from "./pdf.ts";
import { persistExtraction, successFromExisting } from "./persist.ts";
import type { ExtractionRequest } from "./types.ts";
import { assertReadyForExtraction, loadVersionContext } from "./version.ts";
import { validateExtraction } from "./validate.ts";
import { deterministicDemoExtraction } from "./demoFixture.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return errorResponse("VALIDATION_FAILED", "Only POST is supported.", false, 405);
  }
  const auth = await requireOwnerAuth(req);
  if (auth instanceof Response) {
    return auth;
  }

  let body: ExtractionRequest;
  try {
    body = await req.json();
  } catch {
    return errorResponse("VALIDATION_FAILED", "Request body must be JSON.", false, 400);
  }
  const contractVersionId = body.contract_version_id?.trim();
  if (!contractVersionId) {
    return errorResponse("VALIDATION_FAILED", "contract_version_id is required.", false, 400);
  }
  const force = body.force === true;

  const version = await loadVersionContext(
    auth.userClient,
    auth.organizationId,
    contractVersionId,
  );
  if (version instanceof Response) {
    return version;
  }

  if (version.status === "extracted" && version.extraction && !force) {
    return json(successFromExisting(version.id, version.extraction));
  }

  const readyError = assertReadyForExtraction(version, force);
  if (readyError) {
    return readyError;
  }

  const demoFixture = deterministicDemoExtraction(contractVersionId);
  if (demoFixture) {
    const validated = validateExtraction(demoFixture);
    if (!validated.ok) {
      return errorResponse("VALIDATION_FAILED", validated.message, false, 422, validated.details);
    }
    const saved = await persistExtraction(version.id, version.organization_id, validated.extraction);
    if (saved instanceof Response) {
      return saved;
    }
    return json(saved);
  }

  const pdfBytes = await downloadPdf(version.object_path!);
  if (pdfBytes instanceof Response) {
    return pdfBytes;
  }

  const pdfText = await extractPdfText(pdfBytes);
  if (pdfText instanceof Response) {
    return pdfText;
  }

  const llm = await extractWithLlm(version, pdfText);
  if (!llm.ok) {
    return errorResponse("LLM_UNAVAILABLE", llm.message, true, 503);
  }

  const validated = validateExtraction(llm.payload);
  if (!validated.ok) {
    return errorResponse("VALIDATION_FAILED", validated.message, false, 422, validated.details);
  }

  const extraction = attachPipelineMetadata(
    validated.extraction,
    pdfText,
    llm.provider,
    llm.model,
    llm.usedFallback,
  );

  const saved = await persistExtraction(version.id, version.organization_id, extraction);
  if (saved instanceof Response) {
    return saved;
  }
  return json(saved);
});

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
