import { assertEquals, assertFalse, assertThrows } from "jsr:@std/assert";
import ambiguousFrequency from "../_shared/extraction-fixtures/ambiguous_frequency.json" with { type: "json" };
import duplicateRequirements from "../_shared/extraction-fixtures/duplicate_requirements.json" with { type: "json" };
import humanCorrections from "../_shared/extraction-fixtures/human_corrections.json" with { type: "json" };
import invalidRequirementTypes from "../_shared/extraction-fixtures/invalid_requirement_types.json" with { type: "json" };
import missingEvidenceRequirement from "../_shared/extraction-fixtures/missing_evidence_requirement.json" with { type: "json" };
import missingFields from "../_shared/extraction-fixtures/missing_fields.json" with { type: "json" };
import normalContract from "../_shared/extraction-fixtures/normal_contract.json" with { type: "json" };
import unsupportedWording from "../_shared/extraction-fixtures/unsupported_wording.json" with { type: "json" };
import { parseModelJson, validateExtraction } from "./validate.ts";

const VALID_FIXTURES: Record<string, unknown> = {
  normal_contract: normalContract,
  missing_evidence_requirement: missingEvidenceRequirement,
  ambiguous_frequency: ambiguousFrequency,
  unsupported_wording: unsupportedWording,
  human_corrections: humanCorrections,
};

const INVALID_FIXTURES: Record<string, unknown> = {
  missing_fields: missingFields,
  duplicate_requirements: duplicateRequirements,
  invalid_requirement_types: invalidRequirementTypes,
};

for (const [name, payload] of Object.entries(VALID_FIXTURES)) {
  Deno.test(`fixture ${name} passes validation`, () => {
    const result = validateExtraction(payload);
    assertEquals(result.ok, true);
    if (result.ok && result.extraction.visits.length > 0) {
      const visit = result.extraction.visits[0];
      if (visit.start_time.length === 8) {
        assertEquals(visit.start_time.includes(":"), true);
      }
    }
  });
}

for (const [name, payload] of Object.entries(INVALID_FIXTURES)) {
  Deno.test(`fixture ${name} fails validation`, () => {
    const result = validateExtraction(payload);
    assertFalse(result.ok);
  });
}

Deno.test("normal_contract normalizes visit times", () => {
  const result = validateExtraction(normalContract);
  assertEquals(result.ok, true);
  if (result.ok) {
    assertEquals(result.extraction.visits[0].start_time, "08:00:00");
    assertEquals(result.extraction.requirements.length, 2);
  }
});

Deno.test("parseModelJson accepts fenced JSON", () => {
  const parsed = parseModelJson('```json\n{"schema_version":1}\n```');
  assertEquals((parsed as { schema_version: number }).schema_version, 1);
});

Deno.test("parseModelJson accepts raw JSON", () => {
  const parsed = parseModelJson('{"status":"completed"}');
  assertEquals((parsed as { status: string }).status, "completed");
});

Deno.test("parseModelJson rejects truncated JSON", () => {
  assertThrows(() => parseModelJson('{"schema_version":1,'), SyntaxError);
});

Deno.test("parseModelJson rejects non-JSON text", () => {
  assertThrows(() => parseModelJson("not json at all"), SyntaxError);
});

Deno.test("malformed payload after parse fails validation", () => {
  const partial = { schema_version: 1, status: "completed" };
  const result = validateExtraction(partial);
  assertFalse(result.ok);
});
