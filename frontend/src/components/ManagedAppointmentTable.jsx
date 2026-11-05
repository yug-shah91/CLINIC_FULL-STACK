import { useState } from 'react';
import AppointmentTable from './AppointmentTable';
import ConfirmModal from './ConfirmModal';
import useAppointmentActions from '../hooks/useAppointmentActions';
import { formatDateTime } from '../utils/format';

// AppointmentTable + the actions behind its buttons + the "are you sure?" dialog for cancelling.
// Used on the Dashboard and the Appointments page so both behave the same way.
export default function ManagedAppointmentTable({ appointments, reload, showDate = true }) {
  const { confirm, complete, cancel, busyId } = useAppointmentActions(reload);
  const [cancelTarget, setCancelTarget] = useState(null);

  const handleCancel = async () => {
    await cancel(cancelTarget.id);
    setCancelTarget(null);
  };

  return (
    <>
      <AppointmentTable
        appointments={appointments}
        onConfirm={confirm}
        onComplete={complete}
        onCancel={setCancelTarget}
        busyId={busyId}
        showDate={showDate}
      />
      {cancelTarget && (
        <ConfirmModal
          title="Cancel this appointment?"
          confirmLabel="Cancel appointment"
          cancelLabel="Keep it"
          danger
          busy={busyId === cancelTarget.id}
          onConfirm={handleCancel}
          onClose={() => setCancelTarget(null)}
        >
          <p><strong>{cancelTarget.patientName}</strong></p>
          <p>{formatDateTime(cancelTarget.appointmentTime)}</p>
          <p className="muted">The slot becomes free for other patients.</p>
        </ConfirmModal>
      )}
    </>
  );
}
