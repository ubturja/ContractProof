import { createClient } from "@supabase/supabase-js";
import type { ContractExtractionV1, ExtractionSuccessResponse } from "./types.ts";
import { errorResponse } from "./errors.ts";

export async function persistExtraction(
  contractVersionId: string,
  organizationId: string,
  extraction: ContractExtractionV1,
): Promise<ExtractionSuccessResponse | Response> {
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("Supabase service role is not configured.");
  }
  const admin = createClient(supabaseUrl, serviceRoleKey);
  const { data, error } = await admin
    .from("contract_versions")
    .update({
      extraction,
      status: "extracted",
    })
    .eq("id", contractVersionId)
    .eq("organization_id", organizationId)
    .in("status", ["uploaded", "extracted"])
    .select("id")
    .maybeSingle();

  if (error) {
    return errorResponse(
      "DATABASE_UPDATE_FAILED",
      "Extraction could not be saved.",
      true,
      500,
      error,
    );
  }
  if (!data) {
    return errorResponse(
      "DATABASE_UPDATE_FAILED",
      "The contract version was not updated. It may no longer be uploaded.",
      false,
      409,
    );
  }
  return {
    ok: true,
    contract_version_id: contractVersionId,
    status: "extracted",
    requirement_count: extraction.requirements.length,
    visit_count: extraction.visits.length,
    warnings: extraction.document.warnings,
  };
}

export function successFromExisting(
  contractVersionId: string,
  extraction: ContractExtractionV1,
): ExtractionSuccessResponse {
  return {
    ok: true,
    contract_version_id: contractVersionId,
    status: "extracted",
    requirement_count: extraction.requirements.length,
    visit_count: extraction.visits.length,
    warnings: extraction.document.warnings,
  };
}
