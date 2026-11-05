// One number on the dashboard. `tone` picks the colour of the icon badge (see styles).
import CountUp from './CountUp';

export default function StatCard({ icon: Icon, label, value, hint, tone = 'brand' }) {
  return (
    <div className={`stat-card tone-${tone}`}>
      <div className="stat-label">{label}</div>
      <span className="stat-icon"><Icon size={20} aria-hidden="true" /></span>
      <div className="stat-value"><CountUp value={value} /></div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}
