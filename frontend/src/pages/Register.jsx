import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/AuthLayout';
import Field from '../components/Field';
import { EMAIL_RE } from '../utils/validators';

export default function Register() {
  const { user, register } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // New patients go straight to the profile form (the backend needs a profile before booking).
  if (user) return <Navigate to={user.role === 'PATIENT' ? '/profile' : '/'} replace />;

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  // These rules mirror RegisterRequest.java so users get instant feedback.
  const validate = () => {
    const found = {};
    const name = form.name.trim();
    if (name.length < 2 || name.length > 100) found.name = 'Name must be between 2 and 100 characters.';
    if (!EMAIL_RE.test(form.email.trim())) found.email = 'Enter a valid email address.';
    if (form.password.length < 8 || form.password.length > 72 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password))
      found.password = 'Use 8 to 72 characters with at least one letter and one number.';
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length) return;

    setSubmitting(true);
    try {
      await register({ ...form, name: form.name.trim(), email: form.email.trim() });
    } catch (err) {
      setFormError(err.message);
      setErrors(err.fields);
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Create an account"
      subtitle="It takes a minute. You can book your first visit right after."
      footer={<>Already registered? <Link to="/login">Log in</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate>
        {formError && <div className="alert alert-error" role="alert">{formError}</div>}
        <Field label="Full name" htmlFor="name" error={errors.name}>
          <input id="name" name="name" autoComplete="name" value={form.name} onChange={handleChange} />
        </Field>
        <Field label="Email" htmlFor="email" error={errors.email}>
          <input id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={handleChange} />
        </Field>
        <Field label="Password" htmlFor="password" error={errors.password} hint="At least 8 characters, with a letter and a number.">
          <input id="password" name="password" type="password" autoComplete="new-password" value={form.password} onChange={handleChange} />
        </Field>
        <button className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </button>
      </form>
    </AuthLayout>
  );
}
