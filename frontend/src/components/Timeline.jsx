import { formatDate, formatTime } from '../utils/format';

// Vertical list of events, newest first. event = { id, date, tone, icon, title, detail, extra }
// tone is one of: brand, blue, green, red, amber (colours the dot).
export default function Timeline({ events }) {
  return (
    <ol className="timeline">
      {events.map((e) => {
        const Icon = e.icon;
        return (
          <li key={e.id} className={`tl-item tone-${e.tone}`}>
            <span className="tl-dot"><Icon size={14} aria-hidden="true" /></span>
            <div className="tl-body">
              <div className="tl-head">
                <strong>{e.title}</strong>
                <time className="muted" dateTime={e.date}>{formatDate(e.date)}, {formatTime(e.date)}</time>
              </div>
              {e.detail && <p>{e.detail}</p>}
              {e.extra && <p className="muted">{e.extra}</p>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
