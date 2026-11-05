export default function EmptyState({ icon: Icon, title, children }) {
  return (
    <div className="empty">
      {Icon && <span className="empty-icon"><Icon size={26} aria-hidden="true" /></span>}
      <h2>{title}</h2>
      {children && <div className="empty-body">{children}</div>}
    </div>
  );
}
