import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { appointmentApi, patientApi } from '../../api/services';
import { useToast } from '../../context/ToastContext';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Field from '../../components/Field';
import SlotPicker from '../../components/SlotPicker';
import { PageSkeleton } from '../../components/Skeleton';
import ErrorMessage from '../../components/ErrorMessage';
import NeedsProfile from '../../components/NeedsProfile';
import { formatDate, slotLabel } from '../../utils/format';

export default function BookAppointment() {
  const toast = useToast();
  const navigate = useNavigate();
  // Check the profile first so we can show a friendly prompt instead of a failing form.
  const { loading, error: profileError } = useFetch(() => patientApi.getMe(), []);

  const [slot, setSlot] = useState({ date: '', time: '' });
  const [reason, setReason] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const validate = () => {
    const found = {};
    if (!slot.date) found.slot = 'Choose a date.';
    else if (!slot.time) found.slot = 'Choose a time.';
    if (reason.length > 300) found.reason = 'Reason cannot be longer than 300 characters.';
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length) return;

    setSubmitting(true);
    try {
      // The backend expects a LocalDateTime string like "2026-10-20T10:30:00"
      await appointmentApi.book({
        appointmentTime: `${slot.date}T${slot.time}:00`,
        reason: reason.trim() || undefined,
      });
      toast.success('Appointment booked. The doctor will confirm it soon.');
      navigate('/appointments');
    } catch (err) {
      // 409 = someone took the slot a moment ago: show the message and refresh the free slots
      setFormError(err.message);
      setErrors({ reason: err.fields?.reason });
      setSlot((s) => ({ ...s, time: '' }));
      setRefreshKey((k) => k + 1);
      setSubmitting(false);
    }
  };

  if (loading) return <PageSkeleton />;
  if (profileError?.status === 404) return <NeedsProfile />;
  if (profileError) return <ErrorMessage error={profileError} />;

  return (
    <>
      <PageHeader title="Book an appointment" subtitle="Visits are 30 minutes long, Monday to Saturday, 9:00 AM to 5:00 PM." />
      <form className="panel" onSubmit={handleSubmit} noValidate>
        {formError && <div className="alert alert-error" role="alert">{formError}</div>}

        <SlotPicker value={slot} onChange={setSlot} refreshKey={refreshKey} />
        {errors.slot && <p className="field-error" role="alert">{errors.slot}</p>}

        <Field label="Reason for visit (optional)" htmlFor="reason" error={errors.reason} hint={`${reason.length}/300`}>
          <textarea id="reason" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>

        <div className="form-actions">
          <button className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Booking...' : 'Book appointment'}
          </button>
          {slot.date && slot.time && (
            <span className="muted selection-summary">
              {formatDate(`${slot.date}T00:00:00`)} at {slotLabel(slot.time)}
            </span>
          )}
        </div>
      </form>
    </>
  );
}
