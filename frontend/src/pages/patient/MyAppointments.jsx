import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarCheck, CalendarX, FileText } from 'lucide-react';
import { appointmentApi, prescriptionApi } from '../../api/services';
import { useToast } from '../../context/ToastContext';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import { ListSkeleton } from '../../components/Skeleton';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import NeedsProfile from '../../components/NeedsProfile';
import StatusBadge from '../../components/StatusBadge';
import Modal from '../../components/Modal';
import ConfirmModal from '../../components/ConfirmModal';
import Tabs from '../../components/Tabs';
import SlotPicker from '../../components/SlotPicker';
import PrescriptionView from '../../components/PrescriptionView';
import Countdown from '../../components/Countdown';
import { formatDate, formatDateTime, formatTime, isActive, slotLabel } from '../../utils/format';

function PrescriptionModal({ appointment, onClose }) {
  const { data, loading, error } = useFetch(() => prescriptionApi.mineByAppointment(appointment.id), [appointment.id]);
  return (
    <Modal title="Prescription" onClose={onClose} footer={<button className="btn btn-ghost" onClick={onClose}>Close</button>}>
      <p className="muted">{formatDateTime(appointment.appointmentTime)}</p>
      {loading && <ListSkeleton rows={1} />}
      {error && <ErrorMessage error={error} />}
      {data && <PrescriptionView prescription={data} />}
    </Modal>
  );
}

function RescheduleModal({ appointment, onClose, onDone }) {
  const toast = useToast();
  const [slot, setSlot] = useState({ date: '', time: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      await appointmentApi.reschedule(appointment.id, `${slot.date}T${slot.time}:00`);
      toast.success('Appointment rescheduled. The doctor will confirm the new time.');
      onDone();
    } catch (err) {
      setError(err.message);
      setSlot((s) => ({ ...s, time: '' }));
      setRefreshKey((k) => k + 1); // someone may have taken the slot: reload what is free
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Reschedule appointment"
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>Keep current time</button>
          <button className="btn btn-primary" disabled={busy || !slot.date || !slot.time} onClick={submit}>
            {busy ? 'Saving...' : slot.time ? `Move to ${slotLabel(slot.time)}` : 'Choose a new time'}
          </button>
        </>
      }
    >
      <p className="muted">Currently {formatDateTime(appointment.appointmentTime)}</p>
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      <SlotPicker value={slot} onChange={setSlot} refreshKey={refreshKey} />
    </Modal>
  );
}

export default function MyAppointments() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => appointmentApi.mine(), []);
  const [tab, setTab] = useState('upcoming');
  const [cancelTarget, setCancelTarget] = useState(null);
  const [rescheduleTarget, setRescheduleTarget] = useState(null);
  const [rxTarget, setRxTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const confirmCancel = async () => {
    setCancelling(true);
    try {
      await appointmentApi.cancel(cancelTarget.id);
      toast.success('Appointment cancelled.');
      setCancelTarget(null);
      reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setCancelling(false);
    }
  };

  if (error?.status === 404) return <NeedsProfile />;
  if (error) return <ErrorMessage error={error} onRetry={reload} />;
  if (loading && !data) {
    return (<><PageHeader title="Appointments" /><ListSkeleton /></>);
  }

  // Upcoming: soonest first. History: most recent first.
  const upcoming = data.filter((a) => isActive(a.status)).sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime));
  const history = data.filter((a) => !isActive(a.status)).sort((a, b) => b.appointmentTime.localeCompare(a.appointmentTime));
  const next = upcoming[0];
  // On the Upcoming tab the next visit has its own card, so the list only holds the later ones.
  const list = tab === 'upcoming' ? upcoming.slice(1) : history;

  return (
    <>
      <PageHeader
        title="Appointments"
        actions={<Link className="btn btn-primary" to="/appointments/new">Book appointment</Link>}
      />

      <Tabs
        label="Appointments"
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'upcoming', label: 'Upcoming', count: upcoming.length },
          { id: 'history', label: 'History', count: history.length },
        ]}
      />

      {tab === 'upcoming' && next && (
        <section className="next-visit" aria-label="Your next appointment">
          <div>
            <span className="muted">Your next visit <Countdown startsInSeconds={next.startsInSeconds} /></span>
            <h2>{formatDate(next.appointmentTime)} at {formatTime(next.appointmentTime)}</h2>
            {next.reason && <p className="muted">{next.reason}</p>}
            <StatusBadge status={next.status} />
          </div>
          <div className="actions">
            {next.patientCanChange ? (
              <>
                <button className="btn btn-ghost" onClick={() => setRescheduleTarget(next)}>Reschedule</button>
                <button className="btn btn-ghost" onClick={() => setCancelTarget(next)}>Cancel</button>
              </>
            ) : (
              <span className="muted">Changes close {next.changeCutoffHours} hours before the visit. Please call the clinic.</span>
            )}
          </div>
        </section>
      )}

      {tab === 'upcoming' && next && list.length > 0 && <h3 className="list-title">Later visits</h3>}

      {list.length === 0 ? (
        tab === 'upcoming' && next ? null : tab === 'upcoming' ? (
          <EmptyState icon={CalendarCheck} title="No upcoming appointments">
            <p className="muted">Book a visit and it will show up here.</p>
            <Link className="btn btn-primary" to="/appointments/new">Book appointment</Link>
          </EmptyState>
        ) : (
          <EmptyState icon={CalendarX} title="No past appointments yet">
            <p className="muted">Completed and cancelled visits are listed here.</p>
          </EmptyState>
        )
      ) : (
        <ul className="appt-list">
          {list.map((a) => (
            <li key={a.id} className="appt">
              <div className="appt-when">
                <strong>{formatTime(a.appointmentTime)}</strong>
                <span className="muted">{formatDate(a.appointmentTime)}</span>
              </div>
              <div className="appt-reason">{a.reason || <span className="muted">No reason given</span>}</div>
              <StatusBadge status={a.status} />
              <div className="actions">
                {a.patientCanChange && (
                  <>
                    <button className="btn btn-ghost btn-sm" onClick={() => setRescheduleTarget(a)}>Reschedule</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => setCancelTarget(a)}>Cancel</button>
                  </>
                )}
                {a.status === 'COMPLETED' && a.hasPrescription && (
                  <button className="btn btn-ghost btn-sm" onClick={() => setRxTarget(a)}>
                    <FileText size={15} aria-hidden="true" /> Prescription
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {cancelTarget && (
        <ConfirmModal
          title="Cancel this appointment?"
          confirmLabel="Cancel appointment"
          cancelLabel="Keep appointment"
          danger
          busy={cancelling}
          onConfirm={confirmCancel}
          onClose={() => setCancelTarget(null)}
        >
          <p>{formatDateTime(cancelTarget.appointmentTime)}</p>
          <p className="muted">This frees the slot for other patients.</p>
        </ConfirmModal>
      )}

      {rescheduleTarget && (
        <RescheduleModal
          appointment={rescheduleTarget}
          onClose={() => setRescheduleTarget(null)}
          onDone={() => { setRescheduleTarget(null); reload(); }}
        />
      )}

      {rxTarget && <PrescriptionModal appointment={rxTarget} onClose={() => setRxTarget(null)} />}
    </>
  );
}
