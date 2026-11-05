import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock } from 'lucide-react';
import { patientApi } from '../../api/services';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Field from '../../components/Field';
import { PageSkeleton } from '../../components/Skeleton';
import ErrorMessage from '../../components/ErrorMessage';
import { BLOOD_GROUPS, clean, toDateInputValue } from '../../utils/format';
import { PHONE_RE } from '../../utils/validators';

const EMPTY = { name: '', dateOfBirth: '', phone: '', gender: '', bloodGroup: '', address: '', allergies: '', medicalHistory: '' };

export default function Profile() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const { data: profile, loading, error, reload } = useFetch(() => patientApi.getMe(), []);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // 404 just means "no profile yet": the same form then CREATES one instead of updating.
  const isNew = error?.status === 404;

  useEffect(() => {
    if (profile) {
      setForm({
        name: profile.name ?? '',
        dateOfBirth: profile.dateOfBirth ?? '',
        phone: profile.phone ?? '',
        gender: profile.gender ?? '',
        bloodGroup: profile.bloodGroup ?? '',
        address: profile.address ?? '',
        allergies: profile.allergies ?? '',
        medicalHistory: profile.medicalHistory ?? '',
      });
    }
  }, [profile]);

  useEffect(() => {
    if (isNew) setForm((f) => ({ ...f, name: f.name || user.name }));
  }, [isNew, user.name]);

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const validate = () => {
    const found = {};
    const name = form.name.trim();
    if (name && (name.length < 2 || name.length > 100)) found.name = 'Name must be between 2 and 100 characters.';
    if (form.dateOfBirth && form.dateOfBirth > toDateInputValue()) found.dateOfBirth = 'Date of birth cannot be in the future.';
    if (form.phone && !PHONE_RE.test(form.phone)) found.phone = 'Phone must be exactly 10 digits.';
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;

    setSaving(true);
    try {
      const payload = clean({ ...form, name: form.name.trim() });
      if (isNew) {
        await patientApi.createProfile(payload);
        toast.success('Profile created. You can book your first visit now.');
        navigate('/appointments');
      } else {
        await patientApi.updateProfile(payload);
        toast.success('Profile updated.');
        reload();
      }
    } catch (err) {
      toast.error(err.message);
      setErrors(err.fields);
    } finally {
      setSaving(false);
    }
  };

  if (loading && !profile && !isNew) return <PageSkeleton />;
  if (error && !isNew) return <ErrorMessage error={error} onRetry={reload} />;

  return (
    <>
      <PageHeader
        title={isNew ? 'Create your profile' : 'My profile'}
        subtitle={isNew ? 'The doctor uses these details during your visits.' : profile?.email}
      />
      <form className="panel form-grid" onSubmit={handleSubmit} noValidate>
        <Field label="Full name" htmlFor="name" error={errors.name}>
          <input id="name" name="name" value={form.name} onChange={handleChange} />
        </Field>
        <Field label="Date of birth" htmlFor="dateOfBirth" error={errors.dateOfBirth}>
          <input id="dateOfBirth" name="dateOfBirth" type="date" max={toDateInputValue()} value={form.dateOfBirth} onChange={handleChange} />
        </Field>
        <Field label="Phone" htmlFor="phone" error={errors.phone} hint="10 digits, no spaces.">
          <input id="phone" name="phone" inputMode="numeric" maxLength={10} value={form.phone} onChange={handleChange} />
        </Field>
        <Field label="Gender" htmlFor="gender" error={errors.gender}>
          <select id="gender" name="gender" value={form.gender} onChange={handleChange}>
            <option value="">Select</option>
            <option>Male</option>
            <option>Female</option>
            <option>Other</option>
          </select>
        </Field>
        <Field label="Blood group" htmlFor="bloodGroup" error={errors.bloodGroup}>
          <select id="bloodGroup" name="bloodGroup" value={form.bloodGroup} onChange={handleChange}>
            <option value="">Select</option>
            {BLOOD_GROUPS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </Field>
        <div className="span-2">
          <Field label="Address" htmlFor="address" error={errors.address}>
            <textarea id="address" name="address" rows={3} value={form.address} onChange={handleChange} />
          </Field>
        </div>
        <div className="span-2">
          <Field label="Allergies" htmlFor="allergies" error={errors.allergies} hint="Medicines, foods or anything else you react to. Write &quot;None&quot; if you have none.">
            <textarea id="allergies" name="allergies" rows={2} maxLength={1000} value={form.allergies} onChange={handleChange} />
          </Field>
        </div>
        <div className="span-2">
          <Field label="Medical history" htmlFor="medicalHistory" error={errors.medicalHistory} hint="Past illnesses, surgeries, long-term conditions, medicines you take regularly.">
            <textarea id="medicalHistory" name="medicalHistory" rows={4} maxLength={2000} value={form.medicalHistory} onChange={handleChange} />
          </Field>
        </div>
        <p className="muted span-2 privacy-note">
          <Lock size={14} aria-hidden="true" /> Only you and your doctor can see this information.
        </p>
        <div className="span-2">
          <button className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : isNew ? 'Create profile' : 'Save changes'}
          </button>
        </div>
      </form>
    </>
  );
}
