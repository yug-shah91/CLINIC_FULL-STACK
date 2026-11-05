// Shared frame for the Login and Register pages.
// The left panel shows the clinic's real booking rules (taken from AppointmentService).
export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="auth">
      <section className="auth-side">
        <div className="brand brand-light">Clinic</div>
        <div>
          <h2>Book visits and keep your prescriptions in one place.</h2>
          <dl className="hours">
            <dt>Monday to Saturday</dt>
            <dd>9:00 AM to 5:00 PM</dd>
            <dt>Sunday</dt>
            <dd>Closed</dd>
          </dl>
          <p className="auth-note">Visits are 30 minutes long. The last visit of the day starts at 4:30 PM.</p>
        </div>
      </section>
      <section className="auth-main">
        <div className="auth-card">
          <h1>{title}</h1>
          <p className="muted">{subtitle}</p>
          {children}
          <p className="auth-footer">{footer}</p>
        </div>
      </section>
    </div>
  );
}
