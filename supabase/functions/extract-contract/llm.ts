import type { ContractExtractionV1, VersionContext } from "./types.ts";
import type { PdfExtractionResult } from "./pdf.ts";
import { parseModelJson } from "./validate.ts";

const EXTRACTION_INSTRUCTION = `You extract commercial cleaning contract requirements.
Return ONLY valid JSON matching this shape (no markdown):
{
  "schema_version": 1,
  "status": "completed",
  "extracted_at": "<ISO-8601 UTC>",
  "pipeline": { "text_engine": "unpdf", "llm_provider": "gemini|groq", "llm_model": "<model>", "used_fallback": false },
  "document": { "page_count": number, "text_char_count": number, "ocr_page_indexes": number[], "warnings": string[] },
  "visits": [{ "weekday": 1-7, "start_time": "HH:MM", "end_time": "HH:MM", "timezone": "IANA", "starts_on": "YYYY-MM-DD", "ends_on": null, "confidence": 0-1 }],
  "requirements": [{ "key": "stable_id", "task": "...", "requires_photo": boolean, "is_mandatory": boolean, "zone_code": null, "confidence": 0-1, "evidence_quote": null }]
}
Include at least one requirement. Use unique keys like req_1, req_2. Times use 24h HH:MM.`;

export type LlmResult =
  | { ok: true; payload: unknown; provider: "gemini" | "groq"; model: string; usedFallback: boolean }
  | { ok: false; message: string };

export async function extractWithLlm(
  version: VersionContext,
  pdf: PdfExtractionResult,
): Promise<LlmResult> {
  const prompt = buildPrompt(version, pdf);
  const geminiKey = Deno.env.get("GEMINI_API_KEY");
  if (geminiKey) {
    const gemini = await callGemini(geminiKey, prompt);
    if (gemini.ok) {
      return { ok: true, payload: gemini.payload, provider: "gemini", model: gemini.model, usedFallback: false };
    }
  }
  const groqKey = Deno.env.get("GROQ_API_KEY");
  if (groqKey) {
    const groq = await callGroq(groqKey, prompt);
    if (groq.ok) {
      return { ok: true, payload: groq.payload, provider: "groq", model: groq.model, usedFallback: true };
    }
  }
  return { ok: false, message: "No LLM provider returned valid JSON." };
}

function buildPrompt(version: VersionContext, pdf: PdfExtractionResult): string {
  return `${EXTRACTION_INSTRUCTION}

Default timezone: ${version.default_timezone}
Contract starts on: ${version.contract_starts_on}
Document page_count: ${pdf.pageCount}
Document text_char_count: ${pdf.textCharCount}
Document ocr_page_indexes: ${JSON.stringify(pdf.ocrPageIndexes)}
Document warnings: ${JSON.stringify(pdf.warnings)}

Contract text (Markdown):

${pdf.markdown}`;
}

async function callGemini(
  apiKey: string,
  prompt: string,
): Promise<{ ok: true; payload: unknown; model: string } | { ok: false }> {
  const model = Deno.env.get("GEMINI_MODEL") ?? "gemini-2.0-flash";
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: { temperature: 0.1, responseMimeType: "application/json" },
    }),
  });
  if (!response.ok) {
    return { ok: false };
  }
  const body = await response.json();
  const text = body?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text || typeof text !== "string") {
    return { ok: false };
  }
  try {
    return { ok: true, payload: parseModelJson(text), model };
  } catch {
    return { ok: false };
  }
}

async function callGroq(
  apiKey: string,
  prompt: string,
): Promise<{ ok: true; payload: unknown; model: string } | { ok: false }> {
  const model = Deno.env.get("GROQ_MODEL") ?? "llama-3.3-70b-versatile";
  const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model,
      temperature: 0.1,
      messages: [
        { role: "system", content: "You return only JSON for contract requirement extraction." },
        { role: "user", content: prompt },
      ],
    }),
  });
  if (!response.ok) {
    return { ok: false };
  }
  const body = await response.json();
  const text = body?.choices?.[0]?.message?.content;
  if (!text || typeof text !== "string") {
    return { ok: false };
  }
  try {
    return { ok: true, payload: parseModelJson(text), model };
  } catch {
    return { ok: false };
  }
}

export function attachPipelineMetadata(
  extraction: ContractExtractionV1,
  pdf: PdfExtractionResult,
  provider: "gemini" | "groq",
  model: string,
  usedFallback: boolean,
): ContractExtractionV1 {
  return {
    ...extraction,
    extracted_at: extraction.extracted_at || new Date().toISOString(),
    pipeline: {
      text_engine: "unpdf",
      llm_provider: provider,
      llm_model: model,
      used_fallback: usedFallback,
    },
    document: {
      page_count: pdf.pageCount,
      text_char_count: pdf.textCharCount,
      ocr_page_indexes: pdf.ocrPageIndexes,
      warnings: [...pdf.warnings, ...extraction.document.warnings],
    },
  };
}
