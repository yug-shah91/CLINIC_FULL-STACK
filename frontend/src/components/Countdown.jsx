import { useEffect, useMemo, useState } from 'react';
import { Clock } from 'lucide-react';

function describe(ms) {
  if (ms <= 0) return { text: 'Happening now', soon: true };
  const minutes = Math.floor(ms / 60000);
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  if (days >= 1) return { text: `in ${days} day${days > 1 ? 's' : ''}${hours ? ` ${hours}h` : ''}`, soon: false };
  if (hours >= 1) return { text: `in ${hours}h ${minutes % 60}m`, soon: true };
  return { text: `in ${Math.max(1, minutes)} min`, soon: true };
}

// Counts down from `startsInSeconds`, which the API computes on the clinic's clock, so the
// patient's own timezone cannot skew it. We only measure how much time passed since the
// response arrived, using elapsed time rather than a wall-clock comparison.
export default function Countdown({ startsInSeconds }) {
  const base = useMemo(
    () => ({ receivedAt: Date.now(), ms: startsInSeconds * 1000 }),
    [startsInSeconds]
  );
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 30000);
    return () => clearInterval(id);
  }, []);

  const { text, soon } = describe(base.ms - Math.max(0, now - base.receivedAt));
  return (
    <span className={`countdown ${soon ? 'soon' : ''}`}>
      <Clock size={14} aria-hidden="true" /> {text}
    </span>
  );
}
