// Grey shimmering placeholders shown while data loads. They keep the page layout stable,
// so nothing jumps around when the real content arrives.
export function Skeleton({ width = '100%', height = 14, style }) {
  return <span className="skeleton" style={{ width, height, ...style }} aria-hidden="true" />;
}

function Wrapper({ children, label = 'Loading' }) {
  return (
    <div role="status" aria-busy="true">
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

export function CardsSkeleton({ count = 4 }) {
  return (
    <Wrapper>
      <div className="stats-grid">
        {Array.from({ length: count }, (_, i) => (
          <div className="stat-card" key={i}>
            <Skeleton width={40} height={40} style={{ borderRadius: 10 }} />
            <div style={{ flex: 1 }}>
              <Skeleton width="60%" height={12} />
              <Skeleton width="35%" height={26} style={{ marginTop: 10 }} />
            </div>
          </div>
        ))}
      </div>
    </Wrapper>
  );
}

export function TableSkeleton({ rows = 5, cols = 5 }) {
  return (
    <Wrapper>
      <div className="table-wrap">
        <table className="data">
          <tbody>
            {Array.from({ length: rows }, (_, r) => (
              <tr key={r}>
                {Array.from({ length: cols }, (_, c) => (
                  <td key={c}><Skeleton width={c === 0 ? '70%' : '85%'} /></td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Wrapper>
  );
}

export function ListSkeleton({ rows = 4 }) {
  return (
    <Wrapper>
      <ul className="appt-list">
        {Array.from({ length: rows }, (_, i) => (
          <li className="appt" key={i}>
            <div><Skeleton width={70} /><Skeleton width={100} style={{ marginTop: 8 }} /></div>
            <Skeleton width="50%" />
            <Skeleton width={70} height={22} style={{ borderRadius: 999 }} />
            <Skeleton width={110} height={30} style={{ borderRadius: 8 }} />
          </li>
        ))}
      </ul>
    </Wrapper>
  );
}

export function PageSkeleton() {
  return (
    <Wrapper>
      <Skeleton width={220} height={32} />
      <Skeleton width={320} height={14} style={{ marginTop: 12, marginBottom: 28 }} />
      <Skeleton height={180} style={{ borderRadius: 12 }} />
    </Wrapper>
  );
}
