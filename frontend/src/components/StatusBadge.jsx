import { statusLabel } from '../utils/format';

// status is the backend enum: SCHEDULED | CONFIRMED | COMPLETED | CANCELLED
export default function StatusBadge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{statusLabel(status)}</span>;
}
