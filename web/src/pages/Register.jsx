import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Boxes } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Field } from '../components/ui';
import { validateRegister } from '../utils/validation';
import { firebaseErrorMessage } from '../utils/firebaseErrors';

export default function Register() {
  const { register } = useAuth();
  const [values, setValues] = useState({ email: '', displayName: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const found = validateRegister(values);
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      await register(values.email, values.displayName, values.password);
      // PublicOnly redirects to the dashboard once the profile is loaded.
    } catch (err) {
      setFormError(firebaseErrorMessage(err));
      setBusy(false);
    }
  }

  return (
    <div className="page-center">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <div className="auth-brand"><Boxes size={28} aria-hidden="true" /> Inventory Tracking</div>
        <h1>Create account</h1>
        {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
        <Field id="email" label="Email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" value={values.email} onChange={set('email')} />
        </Field>
        <Field id="displayName" label="Display name" error={errors.displayName}>
          <input id="displayName" autoComplete="name" maxLength={120} value={values.displayName} onChange={set('displayName')} />
        </Field>
        <Field id="password" label="Password" error={errors.password} hint="At least 8 characters.">
          <input id="password" type="password" autoComplete="new-password" value={values.password} onChange={set('password')} />
        </Field>
        <Field id="confirmPassword" label="Confirm password" error={errors.confirmPassword}>
          <input id="confirmPassword" type="password" autoComplete="new-password" value={values.confirmPassword} onChange={set('confirmPassword')} />
        </Field>
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
        <p className="muted center">Already registered? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}
