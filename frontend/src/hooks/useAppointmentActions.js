import { useState } from 'react';
import { appointmentApi } from '../api/services';
import { useToast } from '../context/ToastContext';

// Doctor actions on an appointment. Shared by the Dashboard and the Appointments page.
export default function useAppointmentActions(onDone) {
  const toast = useToast();
  const [busyId, setBusyId] = useState(null);

  const run = async (id, request, successMessage) => {
    setBusyId(id);
    try {
      await request(id);
      toast.success(successMessage);
      onDone();
      return true;
    } catch (err) {
      toast.error(err.message);
      return false;
    } finally {
      setBusyId(null);
    }
  };

  return {
    busyId,
    confirm: (id) => run(id, appointmentApi.confirm, 'Appointment confirmed.'),
    complete: (id) => run(id, appointmentApi.complete, 'Appointment marked as completed.'),
    cancel: (id) => run(id, appointmentApi.cancel, 'Appointment cancelled.'),
  };
}
