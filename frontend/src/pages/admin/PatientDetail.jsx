import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  CalendarCheck, CalendarClock, CalendarX, ClipboardList, Lock, Pencil, Pill, TriangleAlert,
} from 'lucide-react';
import { appointmentApi, patientApi, prescriptionApi } from '../../api/services';
import { useToast } from '../../context/ToastContext';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Tabs from '../../components/Tabs';
import Timeline from '../../components/Timeline';
import StatusBadge from '../../components/StatusBadge';
import PrescriptionView from '../../components/PrescriptionView';
import EmptyState from '../../components/EmptyState';
import ErrorMessage from '../../components/ErrorMessage';
import Field from '../../components/Field';
import { PageSkeleton } from '../../components/Skeleton';
import {
  ageFromDob, bloodGroupLabel, formatDate, formatTime, initials, isActive, toDateInputValue,
} from '../../utils/format';

function describePrescription(p) {
  return [
    `Diagnosis: ${p.diagnosis}`,
    `Prescribed: ${p.medicines.map((m) => m.name).join(', ')}`,
    p.followUpDate ? `Follow-up on ${formatDate(p.followUpDate)}` : '',
  ].filter(Boolean).join('. ') + '.';
}

// Turns appointments and prescriptions into one list of events, newest first.
function buildEvents(appointments, prescriptions) {
  const byStatus = {
    COMPLETED: ['green', CalendarCheck, 'Visit completed'],
    CANCELLED: ['red', CalendarX, 'Appointment cancelled'],
    CONFIRMED: ['blue', CalendarClock, 'Upcoming visit (confirmed)'],
    SCHEDULED: ['amber', CalendarClock, 'Upcoming visit (booked)'],
  };
  // A visit and its prescription are ONE event, so the story reads: visit, diagnosis, treatment.
  const rxByAppointment = new Map(prescriptions.map((p) => [p.appointmentId, p]));
  const events = appointments.map((a) => {
    const [tone, icon, title] = byStatus[a.status];
    const rx = rxByAppointment.get(a.id);
    rxByAppointment.delete(a.id);
    return {
      id: `a${a.id}`, date: a.appointmentTime, tone, icon, title, detail: a.reason,
      extra: rx ? describePrescription(rx) : undefined,
    };
  });
  // Safety net: a prescription whose visit is not in the list still gets its own event.
  rxByAppointment.forEach((p) => events.push({
    id: `p${p.id}`, date: p.createdAt, tone: 'brand', icon: Pill, title: 'Prescription issued',
    detail: p.diagnosis, extra: describePrescription(p),
  }));
  return events.sort((a, b) => b.date.localeCompare(a.date));
}

// Long histories start collapsed: the 8 newest events, then "show earlier" adds 10 at a time.
function TimelineSection({ events }) {
  const [shown, setShown] = useState(8);
  return (
    <section className="panel">
      <Timeline events={events.slice(0, shown)} />
      {shown < events.length && (
        <button className="btn btn-ghost btn-sm" onClick={() => setShown((n) => n + 10)}>
          Show earlier events ({events.length - shown} more)
        </button>
      )}
    </section>
  );
}

function MedicalInfo({ patient, onSaved }) {
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ allergies: patient.allergies ?? '', medicalHistory: patient.medicalHistory ?? '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await patientApi.updateMedicalNotes(patient.id, form);
      toast.success('Medical record updated.');
      setEditing(false);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (editing) {
    return (
      <form className="panel" onSubmit={save}>
        <h2>Medical information</h2>
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        <Field label="Allergies" htmlFor="m-allergies">
          <textarea id="m-allergies" rows={2} maxLength={1000} value={form.allergies}
            onChange={(e) => setForm((f) => ({ ...f, allergies: e.target.value }))} />
        </Field>
        <Field label="Medical history" htmlFor="m-history">
          <textarea id="m-history" rows={5} maxLength={2000} value={form.medicalHistory}
            onChange={(e) => setForm((f) => ({ ...f, medicalHistory: e.target.value }))} />
        </Field>
        <div className="form-actions">
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
          <button type="button" className="btn btn-ghost" onClick={() => setEditing(false)}>Cancel</button>
        </div>
      </form>
    );
  }

  return (
    <section className="panel">
      <div className="section-head tight">
        <h2>Medical information</h2>
        <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>
          <Pencil size={14} aria-hidden="true" /> Edit
        </button>
      </div>
      {patient.allergies ? (
        <div className="callout callout-warn">
          <TriangleAlert size={18} aria-hidden="true" />
          <div><strong>Allergies</strong><p>{patient.allergies}</p></div>
        </div>
      ) : (
        <p className="muted">No allergies recorded.</p>
      )}
      <h3>Medical history</h3>
      <p className="preserve">{patient.medicalHistory || <span className="muted">No medical history recorded.</span>}</p>
    </section>
  );
}

export default function PatientDetail() {
  const { id } = useParams(); // from the URL: /admin/patients/:id
  const [tab, setTab] = useState('overview');

  // Promise.all runs the three requests at the same time and waits for all of them.
  const { data, loading, error, reload } = useFetch(
    () => Promise.all([patientApi.getById(id), appointmentApi.forPatient(id), prescriptionApi.historyForPatient(id)]),
    [id]
  );

  if (error) return <ErrorMessage error={error} onRetry={reload} />;
  if (loading && !data) return <PageSkeleton />;

  const [patient, appointments, prescriptions] = data;
  const count = (fn) => appointments.filter(fn).length;
  const age = ageFromDob(patient.dateOfBirth);
  const nextFollowUp = prescriptions
    .map((p) => p.followUpDate)
    .filter((d) => d && d >= toDateInputValue())
    .sort()[0];

  return (
    <>
      <PageHeader
        title={patient.name}
        subtitle={patient.email}
        actions={<Link className="btn btn-ghost" to="/admin/patients">Back to patients</Link>}
      />
      <p className="privacy-banner">
        <Lock size={14} aria-hidden="true" /> Confidential medical record. Visible only to doctors and to the patient.
      </p>

      <div className="record-summary panel">
        <span className="avatar avatar-lg" aria-hidden="true">{initials(patient.name)}</span>
        <dl className="mini-stats">
          <div><dt>Age</dt><dd>{age ?? '-'}</dd></div>
          <div><dt>Blood group</dt><dd>{bloodGroupLabel(patient.bloodGroup)}</dd></div>
          <div><dt>Completed visits</dt><dd>{count((a) => a.status === 'COMPLETED')}</dd></div>
          <div><dt>Upcoming</dt><dd>{count((a) => isActive(a.status))}</dd></div>
          <div><dt>Cancelled</dt><dd>{count((a) => a.status === 'CANCELLED')}</dd></div>
          <div><dt>Prescriptions</dt><dd>{prescriptions.length}</dd></div>
        </dl>
      </div>

      <Tabs
        label="Patient record"
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'overview', label: 'Overview' },
          { id: 'timeline', label: 'Timeline' },
          { id: 'appointments', label: 'Appointments', count: appointments.length },
          { id: 'prescriptions', label: 'Prescriptions', count: prescriptions.length },
        ]}
      />

      {tab === 'overview' && (
        <div className="two-col">
          <section className="panel">
            <h2>Contact and personal details</h2>
            <dl className="kv kv-wide">
              <dt>Phone</dt><dd>{patient.phone || 'Not given'}</dd>
              <dt>Email</dt><dd>{patient.email}</dd>
              <dt>Date of birth</dt><dd>{patient.dateOfBirth ? formatDate(patient.dateOfBirth) : 'Not given'}</dd>
              <dt>Gender</dt><dd>{patient.gender || 'Not given'}</dd>
              <dt>Address</dt><dd>{patient.address || 'Not given'}</dd>
              <dt>Registered</dt><dd>{formatDate(patient.registeredAt)}</dd>
              {nextFollowUp && (<><dt>Next follow-up</dt><dd><strong>{formatDate(nextFollowUp)}</strong></dd></>)}
            </dl>
          </section>
          <MedicalInfo key={`${patient.allergies}|${patient.medicalHistory}`} patient={patient} onSaved={reload} />
        </div>
      )}

      {tab === 'timeline' && (
        appointments.length + prescriptions.length === 0
          ? <EmptyState icon={ClipboardList} title="No history yet"><p className="muted">Visits and prescriptions will appear here.</p></EmptyState>
          : <TimelineSection events={buildEvents(appointments, prescriptions)} />
      )}

      {tab === 'appointments' && (
        appointments.length === 0
          ? <EmptyState icon={CalendarX} title="No appointments yet" />
          : (
            <div className="table-wrap">
              <table className="data">
                <thead><tr><th>When</th><th>Reason</th><th>Status</th><th><span className="sr-only">Prescription</span></th></tr></thead>
                <tbody>
                  {appointments.map((a) => (
                    <tr key={a.id}>
                      <td><strong>{formatTime(a.appointmentTime)}</strong><br /><span className="muted">{formatDate(a.appointmentTime)}</span></td>
                      <td>{a.reason || <span className="muted">Not given</span>}</td>
                      <td><StatusBadge status={a.status} /></td>
                      <td className="actions">
                        {a.status === 'COMPLETED' && (
                          <Link className="btn btn-ghost btn-sm" to={`/admin/prescriptions/${a.id}`}
                            state={{ patientName: patient.name, time: a.appointmentTime }}>
                            {a.hasPrescription ? 'View prescription' : 'Write prescription'}
                          </Link>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
      )}

      {tab === 'prescriptions' && (
        prescriptions.length === 0
          ? <EmptyState icon={Pill} title="No prescriptions yet"><p className="muted">Prescriptions written after a completed visit are listed here.</p></EmptyState>
          : (
            <div className="rx-list">
              {prescriptions.map((p) => (
                <details key={p.id} className="rx">
                  <summary>
                    <span>{formatDate(p.createdAt)}</span>
                    <strong>{p.diagnosis}</strong>
                  </summary>
                  <PrescriptionView prescription={p} />
                </details>
              ))}
            </div>
          )
      )}
    </>
  );
}
