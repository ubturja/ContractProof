import { assertEquals } from "jsr:@std/assert";
import { assertValidSummary } from "./validate.ts";

Deno.test("accepts valid dispute summary", () => {
  const summary = assertValidSummary({
    allegation: "Area was not cleaned.",
    requirements: [
      { requirementText: "Vacuum lobby", contractualContext: "Per contract v1." },
    ],
    recorded_evidence: ["Photo captured 2026-01-15T10:00:00Z"],
    missing_evidence: ["Mop kitchen"],
    exceptions: [],
    neutral_overview: "Records show partial completion.",
  });
  assertEquals(summary.allegation, "Area was not cleaned.");
});

Deno.test("rejects missing allegation", () => {
  let failed = false;
  try {
    assertValidSummary({
      allegation: "",
      requirements: [],
      recorded_evidence: [],
      missing_evidence: [],
      exceptions: [],
      neutral_overview: "x",
    });
  } catch {
    failed = true;
  }
  assertEquals(failed, true);
});
