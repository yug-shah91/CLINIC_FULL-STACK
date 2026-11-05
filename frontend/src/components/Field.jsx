// Label + input + error message, so every form looks and behaves the same.
// Usage: <Field label="Email" htmlFor="email" error={errors.email}><input id="email" .../></Field>
export default function Field({ label, htmlFor, error, hint, children }) {
  return (
    <div className="field">
      <label htmlFor={htmlFor}>{label}</label>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error" role="alert">{error}</span>}
    </div>
  );
}
