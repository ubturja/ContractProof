import { PDFDocument, StandardFonts, rgb } from "pdf-lib";
import type { ReportPdfContext } from "./types.ts";

const MARGIN = 50;
const PAGE_WIDTH = 612;
const PAGE_HEIGHT = 792;
const LINE_HEIGHT = 14;

export async function buildReportPdf(context: ReportPdfContext): Promise<Uint8Array> {
  const doc = await PDFDocument.create();
  doc.setTitle("CONTRACTPROOF Service Evidence Report");
  const font = await doc.embedFont(StandardFonts.Helvetica);
  const fontBold = await doc.embedFont(StandardFonts.HelveticaBold);

  let page = doc.addPage([PAGE_WIDTH, PAGE_HEIGHT]);
  let y = PAGE_HEIGHT - MARGIN;

  const drawTitle = (text: string, size = 18) => {
    page.drawText(text, { x: MARGIN, y, size, font: fontBold, color: rgb(0.1, 0.1, 0.1) });
    y -= size + 8;
  };

  const drawSection = (title: string) => {
    ensureSpace(40);
    page.drawText(title, { x: MARGIN, y, size: 12, font: fontBold });
    y -= LINE_HEIGHT + 4;
  };

  const drawLine = (text: string, size = 10) => {
    const lines = wrapText(text, 90);
    for (const line of lines) {
      ensureSpace(LINE_HEIGHT);
      page.drawText(line, { x: MARGIN, y, size, font });
      y -= LINE_HEIGHT;
    }
  };

  function ensureSpace(needed: number) {
    if (y - needed < MARGIN) {
      page = doc.addPage([PAGE_WIDTH, PAGE_HEIGHT]);
      y = PAGE_HEIGHT - MARGIN;
    }
  }

  drawTitle("CONTRACTPROOF");
  drawTitle("Service Evidence Report", 14);
  y -= 8;

  drawLine(`Client: ${context.header.clientName}`);
  drawLine(`Location: ${context.header.locationName}`);
  drawLine(`Service Date: ${context.header.serviceDate}`);
  drawLine(`Dispute: ${context.header.complaint}`);
  drawLine(`Contract Requirement: ${context.header.disputedRequirement}`);
  y -= 8;

  drawSection("1. Contract Requirement");
  for (const req of context.contractRequirements) {
    const marker = req.disputed ? " (disputed)" : "";
    drawLine(`• ${req.text}${marker}`);
  }

  drawSection("2. Scheduled Service");
  drawLine(`Service date: ${context.scheduledService.serviceDate}`);
  drawLine(`Scheduled: ${context.scheduledService.start} – ${context.scheduledService.end}`);

  drawSection("3. Assigned Personnel");
  drawLine(context.assignedPersonnel);

  drawSection("4. Service Timeline");
  for (const event of context.timeline) {
    drawLine(`${event.at} — ${event.title}`);
    drawLine(`  ${event.body}`);
  }

  drawSection("5. Evidence");
  if (context.evidence.length === 0) {
    drawLine("No uploaded evidence recorded.");
  }
  for (const ev of context.evidence) {
    drawLine(`${ev.requirementText} (${ev.type}) at ${ev.capturedAt}`);
    if (ev.bytes && ev.mimeType?.includes("image")) {
      try {
        const image = ev.mimeType.includes("png")
          ? await doc.embedPng(ev.bytes)
          : await doc.embedJpg(ev.bytes);
        const dims = image.scale(0.35);
        ensureSpace(dims.height + 20);
        page.drawImage(image, {
          x: MARGIN,
          y: y - dims.height,
          width: dims.width,
          height: dims.height,
        });
        y -= dims.height + 12;
      } catch {
        drawLine("  (Image could not be embedded.)");
      }
    }
  }

  drawSection("6. Exceptions");
  if (context.exceptions.length === 0) {
    drawLine("No exceptions recorded.");
  }
  for (const ex of context.exceptions) {
    drawLine(`${ex.requirementText}: ${ex.reason} (${ex.recordedAt})`);
  }

  drawSection("7. Client Acknowledgement");
  for (const ack of context.acknowledgements) {
    drawLine(`${ack.label} at ${ack.at}: ${ack.detail}`);
  }

  drawSection("8. Evidence Coverage");
  for (const row of context.coverage) {
    const label = row.outcome === "missing" ? "Missing" : row.outcome;
    drawLine(`${row.requirementText}: ${label}`);
  }

  drawSection("9. Attachments");
  if (context.attachments.length === 0) {
    drawLine("No attachment paths recorded.");
  }
  for (const att of context.attachments) {
    drawLine(`${att.label}: ${att.path}`);
  }

  drawLine("");
  drawLine("This report is generated from recorded data only. It does not determine fault or legal outcome.");

  return await doc.save();
}

function wrapText(text: string, maxChars: number): string[] {
  const words = text.replace(/\s+/g, " ").trim().split(" ");
  const lines: string[] = [];
  let current = "";
  for (const word of words) {
    const next = current ? `${current} ${word}` : word;
    if (next.length > maxChars) {
      if (current) lines.push(current);
      current = word;
    } else {
      current = next;
    }
  }
  if (current) lines.push(current);
  return lines.length > 0 ? lines : [""];
}
