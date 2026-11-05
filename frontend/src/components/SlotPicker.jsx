import { useState } from 'react';
import { CalendarCheck } from 'lucide-react';
import { appointmentApi } from '../api/services';
import useFetch from '../hooks/useFetch';
import ErrorMessage from './ErrorMessage';
import { Skeleton } from './Skeleton';
import { slotLabel, toDateInputValue } from '../utils/format';

// Date chips + time slots, built from GET /appointments/availability.
//   value:     { date: '2026-10-20', time: '10:30' }   (either may be '')
//   onChange:  called with a new value
//   refreshKey: change it to reload availability (e.g. after a "slot already taken" error)
export default function SlotPicker({ value, onChange, refreshKey = 0 }) {
  const [daysShown, setDaysShown] = useState(14);
  const today = toDateInputValue();
  const { data: days, loading, error, reload } = useFetch(
    () => appointmentApi.availability(today, daysShown),
    [daysShown, refreshKey]
  );

  if (error) return <ErrorMessage error={error} onRetry={reload} />;
  if (loading && !days) {
    return (
      <div className="date-strip" role="status" aria-label="Loading dates">
        {Array.from({ length: 7 }, (_, i) => <Skeleton key={i} width={78} height={74} style={{ borderRadius: 10, flex: 'none' }} />)}
      </div>
    );
  }

  const selectedDay = days.find((d) => d.date === value.date);

  return (
    <div>
      <div className="field">
        <span className="label">Choose a date</span>
        <div className="date-strip" role="group" aria-label="Available dates">
          {days.map((d) => {
            const dt = new Date(`${d.date}T00:00:00`);
            const full = !d.closed && d.availableCount === 0;
            const caption = d.closed ? 'Closed' : full ? 'Full' : `${d.availableCount} free`;
            const selected = d.date === value.date;
            return (
              <button
                key={d.date}
                type="button"
                className={`date-chip ${selected ? 'selected' : ''}`}
                disabled={d.closed || full}
                aria-pressed={selected}
                onClick={() => onChange({ date: d.date, time: '' })}
              >
                <span className="dc-day">{dt.toLocaleDateString('en-GB', { weekday: 'short' })}</span>
                <span className="dc-num">{dt.getDate()}</span>
                <span className="dc-month">{dt.toLocaleDateString('en-GB', { month: 'short' })}</span>
                <span className="dc-caption">{caption}</span>
              </button>
            );
          })}
          {daysShown < 28 && (
            <button type="button" className="date-chip more" onClick={() => setDaysShown(28)}>
              <span>More dates</span>
            </button>
          )}
        </div>
      </div>

      {selectedDay && (
        <div className="field">
          <span className="label">Choose a time</span>
          <div className="slot-grid" role="group" aria-label="Available times">
            {selectedDay.slots.map((s) => (
              <button
                type="button"
                key={s.time}
                className={`slot ${value.time === s.time ? 'selected' : ''}`}
                disabled={!s.available}
                aria-pressed={value.time === s.time}
                title={s.available ? undefined : 'Not available'}
                onClick={() => onChange({ ...value, time: s.time })}
              >
                {slotLabel(s.time)}
              </button>
            ))}
          </div>
          <span className="field-hint">Greyed-out times are already booked or have passed.</span>
        </div>
      )}

      {value.date && value.time && (
        <p className="slot-summary" role="status">
          <CalendarCheck size={18} aria-hidden="true" />
          <span>
            {new Date(`${value.date}T00:00:00`).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}
            {' at '}<strong>{slotLabel(value.time)}</strong>
          </span>
        </p>
      )}
    </div>
  );
}
