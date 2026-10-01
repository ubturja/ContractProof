import type { DisputeContext } from "./types.ts";
import { parseModelJson } from "./validate.ts";

const SUMMARY_INSTRUCTION = `You produce a neutral factual dispute summary from the provided records only.
Return ONLY valid JSON (no markdown) matching:
{
  "allegation": string,
  "requirements": [{ "requirementText": string, "contractualContext": string }],
  "recorded_evidence": string[],
  "missing_evidence": string[],
  "exceptions": string[],
  "neutral_overview": string
}
Rules:
- Separate allegation, contractual requirement, recorded evidence, missing evidence, and exceptions.
- Never invent facts. If something is unknown, write "Not recorded".
- Do not state who is right or wrong. No legal or moral conclusions.
- Use only the input data below.`;

export type LlmResult =
  | { ok: true; payload: unknown; provider: "gemini" | "groq"; model: string }
  | { ok: false; message: string };

export async function summarizeWithLlm(context: DisputeContext): Promise<LlmResult> {
  const prompt = buildPrompt(context);
  const geminiKey = Deno.env.get("GEMINI_API_KEY");
  if (geminiKey) {
    const gemini = await callGemini(geminiKey, prompt);
    if (gemini.ok) {
      return { ok: true, payload: gemini.payload, provider: "gemini", model: gemini.model };
    }
  }
  const groqKey = Deno.env.get("GROQ_API_KEY");
  if (groqKey) {
    const groq = await callGroq(groqKey, prompt);
    if (groq.ok) {
      return { ok: true, payload: groq.payload, provider: "groq", model: groq.model };
    }
  }
  return { ok: false, message: "No LLM provider returned valid JSON." };
}

function buildPrompt(context: DisputeContext): string {
  return `${SUMMARY_INSTRUCTION}

Dispute allegation (complaint):
${context.complaint}

Service date: ${context.serviceDate}
Contract: ${context.contractTitle} (${context.versionLabel})
Disputed requirement (pinned): ${context.disputedRequirementText}
Job started: ${context.jobStartedAt ?? "Not recorded"}
Job completed: ${context.jobCompletedAt ?? "Not recorded"}
Assigned worker: ${context.workerName ?? "Not recorded"}

Per-requirement snapshot at filing:
${JSON.stringify(context.items, null, 2)}`;
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
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      temperature: 0.1,
      messages: [
        { role: "system", content: "Return only valid JSON." },
        { role: "user", content: prompt },
      ],
      response_format: { type: "json_object" },
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
