import { Link } from 'react-router-dom';
import StatusBadge from './StatusBadge';
import { formatDate, formatTime } from '../utils/format';

// Doctor-side table. The buttons depend on the appointment's status:
//   Booked     -> Confirm, Cancel
//   Confirmed  -> Mark completed, Cancel
//   Completed  -> Write prescription (or View prescription if one exists, so it is never written twice)
export default function AppointmentTable({ appointments, onConfirm, onComplete, onCancel, busyId, showDate = true }) {
  return (
    <div className="table-wrap">
      <table className="data stack">
        <thead>
          <tr>
            <th>When</th>
            <th>Patient</th>
            <th>Reason</th>
            <th>Status</th>
            <th><span className="sr-only">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((a) => (
            <tr key={a.id}>
              <td>
                <strong>{formatTime(a.appointmentTime)}</strong>
                {showDate && (<><br /><span className="muted">{formatDate(a.appointmentTime)}</span></>)}
              </td>
              <td><Link to={`/admin/patients/${a.patientId}`}>{a.patientName}</Link></td>
              <td>{a.reason || <span className="muted">Not given</span>}</td>
              <td><StatusBadge status={a.status} /></td>
              <td className="actions">
                {a.status === 'SCHEDULED' && (
                  <button className="btn btn-primary btn-sm" disabled={busyId === a.id} onClick={() => onConfirm(a.id)}>
                    Confirm
                  </button>
                )}
                {a.status === 'CONFIRMED' && (
                  <button className="btn btn-primary btn-sm" disabled={busyId === a.id} onClick={() => onComplete(a.id)}>
                    Mark completed
                  </button>
                )}
                {(a.status === 'SCHEDULED' || a.status === 'CONFIRMED') && (
                  <button className="btn btn-ghost btn-sm" disabled={busyId === a.id} onClick={() => onCancel(a)}>
                    Cancel
                  </button>
                )}
                {a.status === 'COMPLETED' && (
                  <Link
                    className="btn btn-ghost btn-sm"
                    to={`/admin/prescriptions/${a.id}`}
                    state={{ patientName: a.patientName, time: a.appointmentTime }}
                  >
                    {a.hasPrescription ? 'View prescription' : 'Write prescription'}
                  </Link>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
