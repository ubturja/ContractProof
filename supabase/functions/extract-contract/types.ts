export type ExtractionRequest = {
  contract_version_id: string;
  force?: boolean;
};

export type ExtractionErrorCode =
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "VERSION_NOT_IN_REVIEW"
  | "NO_PDF"
  | "STORAGE_DOWNLOAD_FAILED"
  | "TEXT_EXTRACTION_FAILED"
  | "LLM_UNAVAILABLE"
  | "LLM_OUTPUT_INVALID"
  | "VALIDATION_FAILED"
  | "DATABASE_UPDATE_FAILED";

export type ExtractionSuccessResponse = {
  ok: true;
  contract_version_id: string;
  status: "extracted";
  requirement_count: number;
  visit_count: number;
  warnings: string[];
};

export type ExtractionErrorResponse = {
  ok: false;
  error: {
    code: ExtractionErrorCode;
    message: string;
    retryable: boolean;
    details?: unknown;
  };
};

export type ExtractionResponse = ExtractionSuccessResponse | ExtractionErrorResponse;

export type ContractExtractionV1 = {
  schema_version: 1;
  status: "completed";
  extracted_at: string;
  pipeline: {
    text_engine: string;
    llm_provider: "gemini" | "groq";
    llm_model: string;
    used_fallback: boolean;
  };
  document: {
    page_count: number;
    text_char_count: number;
    ocr_page_indexes: number[];
    warnings: string[];
  };
  visits: Array<{
    weekday: number;
    start_time: string;
    end_time: string;
    timezone: string;
    starts_on: string;
    ends_on?: string | null;
    confidence?: number;
  }>;
  requirements: Array<{
    key: string;
    task: string;
    requires_photo: boolean;
    is_mandatory: boolean;
    zone_code?: string | null;
    confidence?: number;
    evidence_quote?: string | null;
  }>;
};

export type VersionContext = {
  id: string;
  organization_id: string;
  contract_id: string;
  status: string;
  bucket: string | null;
  object_path: string | null;
  extraction: ContractExtractionV1 | null;
  default_timezone: string;
  contract_starts_on: string;
};
