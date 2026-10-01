import type { SupabaseClient } from "@supabase/supabase-js";
import type { ContractExtractionV1, VersionContext } from "./types.ts";
import { errorResponse } from "./errors.ts";

type VersionRow = {
  id: string;
  organization_id: string;
  contract_id: string;
  status: string;
  bucket: string | null;
  object_path: string | null;
  extraction: ContractExtractionV1 | null;
  contracts: {
    starts_on: string;
    locations: { timezone: string } | null;
  } | null;
};

export async function loadVersionContext(
  client: SupabaseClient,
  organizationId: string,
  contractVersionId: string,
): Promise<VersionContext | Response> {
  const { data, error } = await client
    .from("contract_versions")
    .select(
      `
      id,
      organization_id,
      contract_id,
      status,
      bucket,
      object_path,
      extraction,
      contracts (
        starts_on,
        locations (
          timezone
        )
      )
    `,
    )
    .eq("id", contractVersionId)
    .eq("organization_id", organizationId)
    .maybeSingle();

  if (error) {
    return errorResponse("NOT_FOUND", "Contract version could not be loaded.", true, 404, error);
  }
  if (!data) {
    return errorResponse("NOT_FOUND", "Contract version was not found.", false, 404);
  }
  const row = data as VersionRow;
  const timezone = row.contracts?.locations?.timezone?.trim();
  if (!timezone) {
    return errorResponse(
      "NOT_FOUND",
      "Contract location timezone is required for extraction.",
      false,
      404,
    );
  }
  const startsOn = row.contracts?.starts_on;
  if (!startsOn) {
    return errorResponse("NOT_FOUND", "Contract start date is required.", false, 404);
  }
  return {
    id: row.id,
    organization_id: row.organization_id,
    contract_id: row.contract_id,
    status: row.status,
    bucket: row.bucket,
    object_path: row.object_path,
    extraction: row.extraction,
    default_timezone: timezone,
    contract_starts_on: startsOn,
  };
}

export function assertReadyForExtraction(version: VersionContext, force: boolean): Response | null {
  if (version.status === "extracted" && force) {
    if (!version.bucket || !version.object_path) {
      return errorResponse("NO_PDF", "This version has no contract PDF.", false, 400);
    }
    return null;
  }
  if (version.status !== "uploaded") {
    return errorResponse(
      "VERSION_NOT_IN_REVIEW",
      "Only an uploaded version can be extracted.",
      false,
      409,
    );
  }
  if (!version.bucket || !version.object_path) {
    return errorResponse("NO_PDF", "This version has no contract PDF.", false, 400);
  }
  return null;
}
