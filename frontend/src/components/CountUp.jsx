import { useEffect, useRef, useState } from 'react';

// Counts from the previous number to the new one. Jumps straight there when the user has
// asked their system for reduced motion.
export default function CountUp({ value, duration = 700 }) {
  const [shown, setShown] = useState(0);
  const last = useRef(0);

  useEffect(() => {
    const target = Number(value);
    if (!Number.isFinite(target)) { setShown(value); return undefined; }
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      last.current = target;
      setShown(target);
      return undefined;
    }
    const begin = last.current;
    const startedAt = performance.now();
    let frame;
    const tick = (now) => {
      const progress = Math.min(1, (now - startedAt) / duration);
      const eased = 1 - (1 - progress) ** 3;
      const current = Math.round(begin + (target - begin) * eased);
      last.current = current;
      setShown(current);
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value, duration]);

  return <>{shown}</>;
}
