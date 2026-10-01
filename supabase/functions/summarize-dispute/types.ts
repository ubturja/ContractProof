export type SummarizeDisputeRequest = {
  dispute_id?: string;
};

export type DisputeSummaryV1 = {
  allegation: string;
  requirements: Array<{ requirementText: string; contractualContext: string }>;
  recorded_evidence: string[];
  missing_evidence: string[];
  exceptions: string[];
  neutral_overview: string;
};

export type DisputeContext = {
  disputeId: string;
  organizationId: string;
  complaint: string;
  serviceDate: string;
  disputedRequirementText: string;
  contractTitle: string;
  versionLabel: string;
  jobStartedAt: string | null;
  jobCompletedAt: string | null;
  workerName: string | null;
  items: Array<{
    requirementText: string;
    outcome: string;
    evidenceCapturedAt: string | null;
    evidenceType: string | null;
    exceptionReason: string | null;
  }>;
};
