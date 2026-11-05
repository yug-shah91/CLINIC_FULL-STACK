import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { formatDate, formatDateTime } from './format';

const TEAL = [14, 90, 87];
const INK = [21, 38, 42];
const MUTED = [91, 110, 115];
const LINE = [218, 227, 228];

// Builds the PDF in memory. `p` is a PrescriptionResponse from the backend.
// Kept separate from downloadPrescriptionPdf so it can be tested without a browser.
export function buildPrescriptionPdf(p) {
  const doc = new jsPDF({ unit: 'mm', format: 'a4' });
  const pageW = doc.internal.pageSize.getWidth();
  const pageH = doc.internal.pageSize.getHeight();
  const m = 18; // page margin
  let y = 22;

  // ---- header
  doc.setFont('helvetica', 'bold').setFontSize(22).setTextColor(...TEAL);
  doc.text('Clinic', m, y);
  doc.setFont('helvetica', 'normal').setFontSize(10).setTextColor(...MUTED);
  doc.text('PRESCRIPTION', pageW - m, y, { align: 'right' });
  y += 5;
  doc.setDrawColor(...TEAL).setLineWidth(0.7).line(m, y, pageW - m, y);

  // ---- patient / visit details
  y += 10;
  const field = (label, value, x) => {
    doc.setFont('helvetica', 'normal').setFontSize(8.5).setTextColor(...MUTED).text(label, x, y);
    doc.setFont('helvetica', 'bold').setFontSize(11).setTextColor(...INK).text(String(value || '-'), x, y + 5.5);
  };
  field('PATIENT', p.patientName, m);
  field('VISIT DATE', p.appointmentTime ? formatDate(p.appointmentTime) : formatDate(p.createdAt), 100);
  field('PRESCRIPTION NO.', `#${p.id}`, 155);
  y += 16;

  // ---- diagnosis
  const section = (title) => {
    doc.setFont('helvetica', 'bold').setFontSize(10).setTextColor(...TEAL).text(title.toUpperCase(), m, y);
    y += 5;
  };
  const paragraph = (text) => {
    doc.setFont('helvetica', 'normal').setFontSize(11).setTextColor(...INK);
    const lines = doc.splitTextToSize(text, pageW - 2 * m);
    doc.text(lines, m, y);
    y += lines.length * 5.2 + 5;
  };

  section('Diagnosis');
  paragraph(p.diagnosis);

  // ---- medicines
  section('Medicines');
  autoTable(doc, {
    startY: y,
    margin: { left: m, right: m },
    head: [['#', 'Medicine', 'Dosage', 'How often', 'Days', 'Instructions']],
    body: (p.medicines || []).map((med, i) => [
      i + 1, med.name, med.dosage || '-', med.frequency || '-', med.durationDays ?? '-', med.instructions || '-',
    ]),
    theme: 'grid',
    styles: { font: 'helvetica', fontSize: 9.5, textColor: INK, lineColor: LINE, lineWidth: 0.2, cellPadding: 2.6 },
    headStyles: { fillColor: TEAL, textColor: 255, fontStyle: 'bold' },
    columnStyles: { 0: { cellWidth: 9 }, 4: { cellWidth: 14 } },
  });
  y = doc.lastAutoTable.finalY + 9;

  // ---- notes and follow-up
  if (p.notes) {
    section('Notes');
    paragraph(p.notes);
  }
  if (p.followUpDate) {
    doc.setFillColor(226, 240, 239).roundedRect(m, y - 4, pageW - 2 * m, 12, 2, 2, 'F');
    doc.setFont('helvetica', 'bold').setFontSize(11).setTextColor(...TEAL);
    doc.text(`Follow-up visit: ${formatDate(p.followUpDate)}`, m + 4, y + 3.5);
    y += 16;
  }

  // ---- signature, kept clear of the table and the footer
  const sigY = Math.max(y + 18, pageH - 48);
  doc.setDrawColor(...MUTED).setLineWidth(0.3).line(pageW - m - 60, sigY, pageW - m, sigY);
  doc.setFont('helvetica', 'normal').setFontSize(9).setTextColor(...MUTED);
  doc.text("Doctor's signature", pageW - m - 30, sigY + 5, { align: 'center' });

  // ---- footer
  doc.setDrawColor(...LINE).setLineWidth(0.3).line(m, pageH - 16, pageW - m, pageH - 16);
  doc.setFontSize(8);
  doc.text(`Written on ${formatDateTime(p.createdAt)}. Valid only with the doctor's signature.`, m, pageH - 11);

  return doc;
}

export function downloadPrescriptionPdf(p) {
  const name = (p.patientName || 'patient').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  buildPrescriptionPdf(p).save(`prescription-${name}-${p.id}.pdf`);
}
