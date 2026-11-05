// Small helpers: dates, labels, and cleaning form data before it goes to the API.

const pad = (n) => String(n).padStart(2, '0');

// "2026-10-20" in the user's LOCAL timezone.
// (new Date().toISOString() gives UTC, which can be a day off late at night.)
export const toDateInputValue = (d = new Date()) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export const formatDate = (iso) =>
  iso
    ? new Date(iso).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
    : '';

export const formatTime = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : '';

export const formatDateTime = (iso) => `${formatDate(iso)}, ${formatTime(iso)}`;

// The backend allows 30-minute slots from 9:00 to 16:30 (see AppointmentService).
export const SLOT_TIMES = Array.from({ length: 16 }, (_, i) => {
  const mins = 9 * 60 + i * 30;
  return `${pad(Math.floor(mins / 60))}:${pad(mins % 60)}`;
});

export const slotLabel = (hhmm) => {
  const [h, m] = hhmm.split(':').map(Number);
  return `${h % 12 || 12}:${pad(m)} ${h < 12 ? 'AM' : 'PM'}`;
};

// Backend enum value -> label shown to the user
export const BLOOD_GROUPS = [
  ['A_POSITIVE', 'A+'], ['A_NEGATIVE', 'A-'],
  ['B_POSITIVE', 'B+'], ['B_NEGATIVE', 'B-'],
  ['AB_POSITIVE', 'AB+'], ['AB_NEGATIVE', 'AB-'],
  ['O_POSITIVE', 'O+'], ['O_NEGATIVE', 'O-'],
];
export const bloodGroupLabel = (value) => BLOOD_GROUPS.find(([k]) => k === value)?.[1] ?? 'Not set';

// SCHEDULED is what the backend calls a booking the doctor has not confirmed yet. Patients see "Booked".
export const STATUS_LABELS = { SCHEDULED: 'Booked', CONFIRMED: 'Confirmed', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
export const statusLabel = (s) => STATUS_LABELS[s] ?? s;
// Booked or confirmed: the visit has not happened yet and can still be changed.
export const isActive = (s) => s === 'SCHEDULED' || s === 'CONFIRMED';


export const formatShortDate = (iso) =>
  iso ? new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' }) : '';

export const initials = (name = '') =>
  name.split(' ').filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');

export const ageFromDob = (dob) => {
  if (!dob) return null;
  const d = new Date(dob);
  const now = new Date();
  let age = now.getFullYear() - d.getFullYear();
  if (now.getMonth() < d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() < d.getDate())) age -= 1;
  return age;
};

// Remove empty strings / nulls. IMPORTANT for this backend: sending phone: "" fails the
// 10-digit validation, but leaving phone out entirely passes. So we drop empty fields.
export const clean = (obj) =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v != null));
