import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2, Plus, Trash2 } from 'lucide-react';
import { prescriptionApi } from '../../api/services';
import { useToast } from '../../context/ToastContext';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Field from '../../components/Field';
import PrescriptionView from '../../components/PrescriptionView';
import ErrorMessage from '../../components/ErrorMessage';
import { PageSkeleton } from '../../components/Skeleton';
import { formatDateTime, toDateInputValue } from '../../utils/format';

const EMPTY_MEDICINE = { name: '', dosage: '', frequency: '', durationDays: '', instructions: '' };
const isBlank = (m) => Object.values(m).every((v) => v.trim() === '');

export default function Prescription() {
  const { appointmentId } = useParams();
  const { state } = useLocation(); // patient name + time, passed along by the link that brought us here
  const navigate = useNavigate();
  const toast = useToast();

  // Is there a prescription already? If yes we show it. A second one can never be created.
  const existing = useFetch(() => prescriptionApi.forAppointment(appointmentId), [appointmentId]);
  const isNew = existing.error?.status === 404;

  const [diagnosis, setDiagnosis] = useState('');
  const [notes, setNotes] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [medicines, setMedicines] = useState([{ ...EMPTY_MEDICINE }]);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  // Immutable updates: always create a NEW array/object, never edit state in place.
  const updateMedicine = (index, field, value) =>
    setMedicines((list) => list.map((m, i) => (i === index ? { ...m, [field]: value } : m)));
  const addMedicine = () => setMedicines((list) => [...list, { ...EMPTY_MEDICINE }]);
  const removeMedicine = (index) =>
    setMedicines((list) => (list.length === 1 ? [{ ...EMPTY_MEDICINE }] : list.filter((_, i) => i !== index)));

  // Same rules as PrescriptionRequest.java, so mistakes are caught before the request is sent.
  const validate = () => {
    const found = {};
    if (!diagnosis.trim()) found.diagnosis = 'Diagnosis is required.';
    if (followUpDate && followUpDate < toDateInputValue()) found.followUpDate = 'Follow-up date cannot be in the past.';
    const filled = medicines.map((m, i) => ({ m, i })).filter(({ m }) => !isBlank(m));
    if (filled.length === 0) found.medicines = 'Add at least one medicine.';
    filled.forEach(({ m, i }) => {
      if (!m.name.trim()) found[`name${i}`] = 'Enter the medicine name.';
      if (!m.dosage.trim()) found[`dosage${i}`] = 'Enter the dosage.';
      if (!m.frequency.trim()) found[`freq${i}`] = 'Enter how often to take it.';
      if (!/^[1-9]\d*$/.test(m.durationDays) || Number(m.durationDays) > 365) found[`days${i}`] = 'Enter 1 to 365 days.';
    });
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length) return;

    setSaving(true);
    try {
      await prescriptionApi.write({
        appointmentId: Number(appointmentId),
        diagnosis: diagnosis.trim(),
        notes: notes.trim() || undefined, // undefined fields are left out of the JSON
        followUpDate: followUpDate || undefined,
        medicines: medicines
          .filter((m) => !isBlank(m))
          .map((m) => ({
            name: m.name.trim(),
            dosage: m.dosage.trim(),
            frequency: m.frequency.trim(),
            durationDays: Number(m.durationDays),
            instructions: m.instructions.trim() || undefined,
          })),
      });
      toast.success('Prescription saved.');
      existing.reload(); // the page now shows the saved prescription
    } catch (err) {
      const fieldMessages = Object.values(err.fields || {});
      setFormError(fieldMessages.length ? fieldMessages.join(' ') : err.message);
    } finally {
      setSaving(false);
    }
  };

  if (existing.loading && !existing.data && !isNew) return <PageSkeleton />;
  if (existing.error && !isNew) return <ErrorMessage error={existing.error} onRetry={existing.reload} />;

  const subtitle = existing.data?.patientName
    ? `${existing.data.patientName}, ${formatDateTime(existing.data.appointmentTime)}`
    : state?.patientName ? `${state.patientName}, ${formatDateTime(state.time)}` : `Appointment #${appointmentId}`;
  const back = <Link className="btn btn-ghost" to="/admin/appointments">Back to appointments</Link>;

  // ---- A prescription exists: show it (read-only)
  if (existing.data) {
    return (
      <>
        <PageHeader title="Prescription" subtitle={subtitle} actions={back} />
        <p className="callout callout-ok">
          <CheckCircle2 size={18} aria-hidden="true" />
          <span>This visit already has a prescription. It is shown below and cannot be written a second time.</span>
        </p>
        <section className="panel"><PrescriptionView prescription={existing.data} /></section>
      </>
    );
  }

  // ---- No prescription yet: the form
  return (
    <>
      <PageHeader title="Write prescription" subtitle={subtitle} actions={back} />
      <form className="panel" onSubmit={handleSubmit} noValidate>
        {formError && <div className="alert alert-error" role="alert">{formError}</div>}

        <div className="form-grid">
          <div className="span-2">
            <Field label="Diagnosis" htmlFor="diagnosis" error={errors.diagnosis}>
              <input id="diagnosis" maxLength={500} value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} />
            </Field>
          </div>
          <Field label="Follow-up date (optional)" htmlFor="followUp" error={errors.followUpDate}>
            <input id="followUp" type="date" min={toDateInputValue()} value={followUpDate} onChange={(e) => setFollowUpDate(e.target.value)} />
          </Field>
          <div className="span-2">
            <Field label="Notes (optional)" htmlFor="notes">
              <textarea id="notes" rows={3} maxLength={1000} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </Field>
          </div>
        </div>

        <h2 className="section-title">Medicines</h2>
        {errors.medicines && <p className="field-error" role="alert">{errors.medicines}</p>}
        <datalist id="frequencies">
          <option value="Once a day" /><option value="Twice a day" /><option value="Three times a day" />
          <option value="At night" /><option value="Every 8 hours" /><option value="When needed" />
        </datalist>

        {medicines.map((m, i) => (
          <fieldset key={i} className="medicine">
            <legend>Medicine {i + 1}</legend>
            <div className="form-grid">
              <Field label="Name" htmlFor={`name${i}`} error={errors[`name${i}`]}>
                <input id={`name${i}`} maxLength={200} value={m.name} onChange={(e) => updateMedicine(i, 'name', e.target.value)} />
              </Field>
              <Field label="Dosage" htmlFor={`dosage${i}`} error={errors[`dosage${i}`]}>
                <input id={`dosage${i}`} placeholder="500 mg" maxLength={50} value={m.dosage} onChange={(e) => updateMedicine(i, 'dosage', e.target.value)} />
              </Field>
              <Field label="How often" htmlFor={`freq${i}`} error={errors[`freq${i}`]}>
                <input id={`freq${i}`} list="frequencies" placeholder="Twice a day" maxLength={100} value={m.frequency} onChange={(e) => updateMedicine(i, 'frequency', e.target.value)} />
              </Field>
              <Field label="Number of days" htmlFor={`days${i}`} error={errors[`days${i}`]}>
                <input id={`days${i}`} inputMode="numeric" value={m.durationDays} onChange={(e) => updateMedicine(i, 'durationDays', e.target.value)} />
              </Field>
              <div className="span-2">
                <Field label="Instructions (optional)" htmlFor={`ins${i}`}>
                  <input id={`ins${i}`} placeholder="After meals" maxLength={300} value={m.instructions} onChange={(e) => updateMedicine(i, 'instructions', e.target.value)} />
                </Field>
              </div>
            </div>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => removeMedicine(i)}>
              <Trash2 size={14} aria-hidden="true" /> Remove medicine
            </button>
          </fieldset>
        ))}

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={addMedicine}>
            <Plus size={16} aria-hidden="true" /> Add medicine
          </button>
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save prescription'}</button>
        </div>
      </form>
    </>
  );
}
