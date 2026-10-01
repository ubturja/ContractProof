import Ajv2020 from "npm:ajv@8.17.1/dist/2020.js";
import schema from "../_shared/extraction-v1.schema.json" with { type: "json" };
import type { ContractExtractionV1 } from "./types.ts";

const ajv = new Ajv2020({ allErrors: true, strict: false });
const validateSchema = ajv.compile(schema);

const clockTime = /^(\d{2}):(\d{2})(:\d{2})?$/;
const isoDate = /^\d{4}-\d{2}-\d{2}$/;

export function validateExtraction(
  value: unknown,
): { ok: true; extraction: ContractExtractionV1 } | { ok: false; message: string; details: unknown } {
  if (!validateSchema(value)) {
    return {
      ok: false,
      message: "Extraction JSON does not match the schema.",
      details: validateSchema.errors,
    };
  }
  const extraction = value as ContractExtractionV1;
  try {
    const normalized = applyBusinessRules(extraction);
    return { ok: true, extraction: normalized };
  } catch (error) {
    return {
      ok: false,
      message: error instanceof Error ? error.message : "Extraction failed business validation.",
      details: error,
    };
  }
}

function applyBusinessRules(extraction: ContractExtractionV1): ContractExtractionV1 {
  if (extraction.schema_version !== 1) {
    throw new Error("Extraction schema version is not supported.");
  }
  if (extraction.status !== "completed") {
    throw new Error("Extraction status must be completed.");
  }
  if (!extraction.extracted_at.trim()) {
    throw new Error("Extraction timestamp is required.");
  }
  const keys = new Set<string>();
  const requirements = extraction.requirements.map((requirement) => {
    const key = requirement.key.trim();
    if (!key) {
      throw new Error("Requirement key is required.");
    }
    if (keys.has(key)) {
      throw new Error("Requirement keys must be unique.");
    }
    keys.add(key);
    const task = requirement.task.trim();
    if (!task) {
      throw new Error("Requirement text is required.");
    }
    if (requirement.confidence != null) {
      requireConfidence(requirement.confidence);
    }
    return { ...requirement, key, task };
  });
  const visits = extraction.visits.map((visit) => {
    if (visit.confidence != null) {
      requireConfidence(visit.confidence);
    }
    return {
      ...visit,
      start_time: normalizeTime(visit.start_time, "Start time"),
      end_time: normalizeTime(visit.end_time, "End time"),
      timezone: requireNonEmpty(visit.timezone, "Visit timezone"),
      starts_on: requireIsoDate(visit.starts_on, "Visit start date"),
      ends_on: visit.ends_on?.trim() ? requireIsoDate(visit.ends_on, "Visit end date") : null,
      weekday: requireWeekday(visit.weekday),
    };
  });
  visits.forEach((visit) => {
    if (visit.end_time <= visit.start_time) {
      throw new Error("Service window must end after it starts.");
    }
    if (visit.ends_on && visit.ends_on < visit.starts_on) {
      throw new Error("Visit end date cannot be before the start date.");
    }
  });
  return { ...extraction, requirements, visits };
}

function requireConfidence(value: number) {
  if (value < 0 || value > 1) {
    throw new Error("Confidence must be between 0 and 1.");
  }
}

function requireWeekday(weekday: number): number {
  if (weekday < 1 || weekday > 7) {
    throw new Error("Visit weekday must be 1 through 7.");
  }
  return weekday;
}

function requireNonEmpty(value: string, label: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    throw new Error(`${label} is required.`);
  }
  return trimmed;
}

function requireIsoDate(value: string, label: string): string {
  const trimmed = value.trim();
  if (!isoDate.test(trimmed)) {
    throw new Error(`${label} must be YYYY-MM-DD.`);
  }
  return trimmed;
}

function normalizeTime(value: string, label: string): string {
  const trimmed = value.trim();
  if (!clockTime.test(trimmed)) {
    throw new Error(`${label} must be a time as HH:MM or HH:MM:SS.`);
  }
  return trimmed.length === 5 ? `${trimmed}:00` : trimmed;
}

export function parseModelJson(raw: string): unknown {
  const trimmed = raw.trim();
  const fenced = trimmed.match(/^```(?:json)?\s*([\s\S]*?)```$/i);
  const body = fenced ? fenced[1].trim() : trimmed;
  return JSON.parse(body);
}
