import type { ContractExtractionV1 } from "./types.ts";
import meridianFixture from "../_shared/extraction-fixtures/meridian_nightly_demo.json" with {
  type: "json",
};

const DEMO_VERSION_FIXTURES: Record<string, ContractExtractionV1> = {
  "eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01": meridianFixture as ContractExtractionV1,
};

export function deterministicDemoExtraction(
  contractVersionId: string,
): ContractExtractionV1 | null {
  if (Deno.env.get("DEMO_DETERMINISTIC_EXTRACTION") !== "true") {
    return null;
  }
  return DEMO_VERSION_FIXTURES[contractVersionId] ?? null;
}
