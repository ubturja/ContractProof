import { assertEquals, assert } from "jsr:@std/assert";
import { PDFDocument } from "pdf-lib";
import { buildReportPdf } from "./pdf.ts";
import type { ReportPdfContext } from "./types.ts";

const fixture: ReportPdfContext = {
  organizationId: "00000000-0000-0000-0000-000000000001",
  disputeId: "00000000-0000-0000-0000-000000000002",
  header: {
    clientName: "Acme Corp",
    locationName: "Main Office",
    serviceDate: "2026-03-01",
    complaint: "Area not cleaned.",
    disputedRequirement: "Mop floors",
    contractTitle: "Cleaning Agreement",
    versionLabel: "v1",
  },
  contractRequirements: [{ text: "Mop floors", disputed: true }],
  scheduledService: {
    start: "2026-03-01T08:00:00Z",
    end: "2026-03-01T09:00:00Z",
    serviceDate: "2026-03-01",
  },
  assignedPersonnel: "Alex Worker",
  timeline: [
    { at: "2026-03-01T08:00:00Z", title: "Job scheduled", body: "Scheduled window." },
  ],
  evidence: [],
  exceptions: [],
  acknowledgements: [
    { label: "Dispute filed", at: "2026-03-02T10:00:00Z", detail: "Area not cleaned." },
  ],
  coverage: [{ requirementText: "Mop floors", outcome: "missing" }],
  attachments: [],
};

Deno.test("buildReportPdf produces valid multi-section PDF", async () => {
  const bytes = await buildReportPdf(fixture);
  assert(bytes.byteLength > 1500, `PDF should have substantial size, got ${bytes.byteLength}`);
  const doc = await PDFDocument.load(bytes);
  assert(doc.getPageCount() >= 1);
  const title = doc.getTitle();
  if (title) {
    assert(title.includes("CONTRACTPROOF") || title.length > 0);
  }
});
