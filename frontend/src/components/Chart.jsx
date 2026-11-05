import { useLayoutEffect, useRef, useState } from 'react';

// A small bar chart drawn with plain SVG (no chart library).
//   buckets: [{ label: '16 Oct', values: { completed: 3, cancelled: 1 } }]
//   series:  [{ key: 'completed', label: 'Completed', color: '#2f8f5b' }]  (bars are stacked in this order)
//   maxValue: optional fixed top of the axis (e.g. 100 for percentages)
const HEIGHT = 260;
const M = { top: 12, right: 12, bottom: 30, left: 38 };

// Round the top of the axis to a friendly number: 7 becomes 8 (steps of 2), 43 becomes 50 (steps of 10)
export function niceScale(max, ticks = 4) {
  if (max <= 0) return { max: ticks, step: 1 };
  const rough = max / ticks;
  const mag = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 5, 10].map((m) => m * mag).find((s) => s * ticks >= max);
  return { max: step * ticks, step };
}

// Measures how wide the chart's box is, so the SVG can fill it exactly (text stays the same size).
function useWidth() {
  const ref = useRef(null);
  const [width, setWidth] = useState(640);
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    const update = () => setWidth(el.clientWidth || 640);
    update();
    if (typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  return [ref, width];
}

export default function Chart({ buckets, series, yFormat = (v) => v, maxValue, description }) {
  const [ref, boxWidth] = useWidth();
  const width = Math.max(boxWidth, buckets.length * 38 + M.left + M.right); // scrolls sideways on phones
  const plotW = width - M.left - M.right;
  const plotH = HEIGHT - M.top - M.bottom;

  const totals = buckets.map((b) => series.reduce((sum, s) => sum + (b.values[s.key] || 0), 0));
  const { max, step } = maxValue ? { max: maxValue, step: maxValue / 4 } : niceScale(Math.max(0, ...totals));
  const y = (v) => M.top + plotH - (v / max) * plotH;
  const slot = plotW / buckets.length;
  const barW = Math.min(36, slot * 0.62);
  const gridValues = Array.from({ length: Math.round(max / step) + 1 }, (_, i) => i * step);
  const labelEvery = slot >= 50 ? 1 : 2;

  return (
    <div className="chart-scroll" ref={ref}>
      <svg width={width} height={HEIGHT} role="img" aria-label={description}>
        {gridValues.map((v) => (
          <g key={v}>
            <line x1={M.left} x2={width - M.right} y1={y(v)} y2={y(v)} className="chart-grid" />
            <text x={M.left - 8} y={y(v) + 4} textAnchor="end" className="chart-axis">{yFormat(v)}</text>
          </g>
        ))}

        {buckets.map((b, i) => {
          const x = M.left + i * slot + (slot - barW) / 2;
          let acc = 0;
          // Count labels from the right, so the newest bar (today / this week / this month) is always labelled.
          const showLabel = (buckets.length - 1 - i) % labelEvery === 0;
          return (
            <g key={b.label + i}>
              <title>{`${b.label}: ${series.map((s) => `${s.label} ${yFormat(b.values[s.key] || 0)}`).join(', ')}`}</title>
              {series.map((s) => {
                const v = b.values[s.key] || 0;
                if (v <= 0) return null;
                const top = y(acc + v);
                const h = y(acc) - top;
                acc += v;
                return <rect key={s.key} x={x} y={top} width={barW} height={h} rx={2} fill={s.color} />;
              })}
              {showLabel && (
                <text x={x + barW / 2} y={HEIGHT - 10} textAnchor="middle" className="chart-axis">{b.label}</text>
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

export function Legend({ series }) {
  return (
    <ul className="legend">
      {series.map((s) => (
        <li key={s.key}><span className="legend-swatch" style={{ background: s.color }} />{s.label}</li>
      ))}
    </ul>
  );
}
