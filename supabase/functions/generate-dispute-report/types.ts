export type GenerateReportRequest = {
  dispute_id?: string;
  force?: boolean;
};

export type ReportPdfContext = {
  organizationId: string;
  disputeId: string;
  header: {
    clientName: string;
    locationName: string;
    serviceDate: string;
    complaint: string;
    disputedRequirement: string;
    contractTitle: string;
    versionLabel: string;
  };
  contractRequirements: Array<{ text: string; disputed: boolean }>;
  scheduledService: { start: string; end: string; serviceDate: string };
  assignedPersonnel: string;
  timeline: Array<{ at: string; title: string; body: string }>;
  evidence: Array<{
    requirementText: string;
    type: string;
    capturedAt: string;
    objectPath: string | null;
    mimeType: string | null;
    bytes: Uint8Array | null;
  }>;
  exceptions: Array<{ requirementText: string; reason: string; recordedAt: string }>;
  acknowledgements: Array<{ label: string; at: string; detail: string }>;
  coverage: Array<{ requirementText: string; outcome: string }>;
  attachments: Array<{ label: string; path: string; mimeType: string | null }>;
};

export type ReportRow = {
  id: string;
  organization_id: string;
  dispute_id: string;
  status: string;
  bucket: string;
  object_path: string | null;
  generated_by: string | null;
  generated_at: string | null;
  failure_reason: string | null;
};
