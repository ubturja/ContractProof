import type { ExtractionErrorCode, ExtractionErrorResponse } from "./types.ts";

export function errorResponse(
  code: ExtractionErrorCode,
  message: string,
  retryable: boolean,
  status: number,
  details?: unknown,
): Response {
  const body: ExtractionErrorResponse = {
    ok: false,
    error: { code, message, retryable, details },
  };
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
