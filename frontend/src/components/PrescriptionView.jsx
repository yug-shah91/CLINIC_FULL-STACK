import { useState } from 'react';
import { CalendarClock, FileDown } from 'lucide-react';
import { formatDate, formatDateTime } from '../utils/format';

// Read-only prescription with a "Download PDF" button.
// Used in the patient's dialog, the doctor's prescription page and the patient record.
export default function PrescriptionView({ prescription: p }) {
  const [busy, setBusy] = useState(false);

  const download = async () => {
    setBusy(true);
    try {
      // Loaded on demand: the PDF library is big and most visits never need it.
      const { downloadPrescriptionPdf } = await import('../utils/prescriptionPdf');
      downloadPrescriptionPdf(p);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="prescription">
      <dl className="kv">
        <dt>Diagnosis</dt>
        <dd>{p.diagnosis}</dd>
        {p.notes && (<><dt>Notes</dt><dd>{p.notes}</dd></>)}
        <dt>Written on</dt>
        <dd>{formatDateTime(p.createdAt)}</dd>
      </dl>

      {p.medicines?.length > 0 ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>Medicine</th><th>Dosage</th><th>How often</th><th>Days</th><th>Instructions</th></tr>
            </thead>
            <tbody>
              {p.medicines.map((m) => (
                <tr key={m.id}>
                  <td><strong>{m.name}</strong></td>
                  <td>{m.dosage || '-'}</td>
                  <td>{m.frequency || '-'}</td>
                  <td>{m.durationDays ?? '-'}</td>
                  <td>{m.instructions || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="muted">No medicines were prescribed.</p>
      )}

      {p.followUpDate && (
        <p className="followup"><CalendarClock size={16} aria-hidden="true" /> Follow-up visit on <strong>{formatDate(p.followUpDate)}</strong></p>
      )}

      <button className="btn btn-ghost btn-sm" onClick={download} disabled={busy}>
        <FileDown size={16} aria-hidden="true" /> {busy ? 'Preparing...' : 'Download PDF'}
      </button>
    </div>
  );
}
