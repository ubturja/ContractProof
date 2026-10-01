import { createClient } from "@supabase/supabase-js";
import { extractText, getDocumentProxy } from "unpdf";

export type PdfExtractionResult = {
  markdown: string;
  pageCount: number;
  textCharCount: number;
  ocrPageIndexes: number[];
  warnings: string[];
};

export async function downloadPdf(objectPath: string): Promise<Uint8Array | Response> {
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("Supabase service role is not configured.");
  }
  const admin = createClient(supabaseUrl, serviceRoleKey);
  const { data, error } = await admin.storage.from("contracts").download(objectPath);
  if (error || !data) {
    return new Response(
      JSON.stringify({
        ok: false,
        error: {
          code: "STORAGE_DOWNLOAD_FAILED",
          message: "The contract PDF could not be downloaded.",
          retryable: true,
        },
      }),
      { status: 502, headers: { "Content-Type": "application/json" } },
    );
  }
  const buffer = await data.arrayBuffer();
  return new Uint8Array(buffer);
}

export async function extractPdfText(bytes: Uint8Array): Promise<PdfExtractionResult | Response> {
  try {
    const pdf = await getDocumentProxy(bytes);
    const { totalPages, text } = await extractText(pdf, { mergePages: false });
    const pageCount = totalPages;
    const ocrPageIndexes: number[] = [];
    const sections: string[] = [];
    const warnings: string[] = [];
    const pages = Array.isArray(text) ? text : [text];
    for (let index = 0; index < pageCount; index++) {
      const pageText = (pages[index] ?? "").trim();
      if (pageText.length === 0) {
        ocrPageIndexes.push(index + 1);
        warnings.push(`Page ${index + 1} has no text layer. OCR was not run.`);
        sections.push(`## Page ${index + 1}\n\n_(no text layer)_\n`);
      } else {
        sections.push(`## Page ${index + 1}\n\n${pageText}\n`);
      }
    }
    const markdown = sections.join("\n");
    const textCharCount = markdown.replace(/\s+/g, " ").trim().length;
    if (textCharCount === 0) {
      return new Response(
        JSON.stringify({
          ok: false,
          error: {
            code: "TEXT_EXTRACTION_FAILED",
            message: "No text could be read from the contract PDF.",
            retryable: false,
          },
        }),
        { status: 422, headers: { "Content-Type": "application/json" } },
      );
    }
    return {
      markdown,
      pageCount,
      textCharCount,
      ocrPageIndexes,
      warnings,
    };
  } catch (cause) {
    return new Response(
      JSON.stringify({
        ok: false,
        error: {
          code: "TEXT_EXTRACTION_FAILED",
          message: "The contract PDF could not be parsed.",
          retryable: false,
          details: String(cause),
        },
      }),
      { status: 422, headers: { "Content-Type": "application/json" } },
    );
  }
}
