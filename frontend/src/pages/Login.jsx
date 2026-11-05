import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/AuthLayout';
import Field from '../components/Field';
import { EMAIL_RE } from '../utils/validators';

export default function Login() {
  const { user, login } = useAuth();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});     // per-field messages
  const [formError, setFormError] = useState(''); // message from the server
  const [submitting, setSubmitting] = useState(false);

  // Once login() succeeds, `user` is set and we land here again: send them onward.
  // If a protected page sent them to /login, go back to that page.
  if (user) return <Navigate to={location.state?.from?.pathname || '/'} replace />;

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const validate = () => {
    const found = {};
    if (!form.email.trim()) found.email = 'Enter your email.';
    else if (!EMAIL_RE.test(form.email.trim())) found.email = 'Enter a valid email address.';
    if (!form.password) found.password = 'Enter your password.';
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
      await login({ email: form.email.trim(), password: form.password });
    } catch (err) {
      setFormError(err.message);
      setErrors(err.fields);
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Log in"
      subtitle="Welcome back. Enter your details to continue."
      footer={<>New here? <Link to="/register">Create an account</Link></>}
    >
      <form onSubmit={handleSubmit} noValidate>
        {formError && <div className="alert alert-error" role="alert">{formError}</div>}
        <Field label="Email" htmlFor="email" error={errors.email}>
          <input id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={handleChange} />
        </Field>
        <Field label="Password" htmlFor="password" error={errors.password}>
          <input id="password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={handleChange} />
        </Field>
        <button className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>
      </form>
    </AuthLayout>
  );
}
