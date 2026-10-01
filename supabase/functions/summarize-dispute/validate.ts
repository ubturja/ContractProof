import Ajv from "ajv";
import schema from "../_shared/dispute-summary-v1.schema.json" with { type: "json" };
import type { DisputeSummaryV1 } from "./types.ts";

const ajv = new Ajv({ allErrors: true, strict: false });
const validate = ajv.compile(schema);

export function parseModelJson(text: string): unknown {
  const trimmed = text.trim();
  const start = trimmed.indexOf("{");
  const end = trimmed.lastIndexOf("}");
  if (start < 0 || end < 0) {
    throw new Error("Model output is not JSON.");
  }
  return JSON.parse(trimmed.slice(start, end + 1));
}

export function assertValidSummary(payload: unknown): DisputeSummaryV1 {
  if (!validate(payload)) {
    const detail = validate.errors?.map((e) => e.message).join("; ") ?? "Invalid summary.";
    throw new Error(detail);
  }
  return payload as DisputeSummaryV1;
}
