import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Boxes } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Field } from '../components/ui';
import { validateLogin } from '../utils/validation';
import { firebaseErrorMessage } from '../utils/firebaseErrors';

export default function Login() {
  const { login, user, profile, error: profileError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (user && profile) navigate(location.state?.from?.pathname || '/', { replace: true });
  }, [user, profile, navigate, location.state]);

  useEffect(() => {
    if (user && profileError) {
      setFormError(profileError.message);
      setBusy(false);
    }
  }, [user, profileError]);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const found = validateLogin(values);
    setErrors(found);
    setFormError('');
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      await login(values.email, values.password);
    } catch (err) {
      setFormError(firebaseErrorMessage(err));
      setBusy(false);
    }
  }

  return (
    <div className="page-center">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <div className="auth-brand"><Boxes size={28} aria-hidden="true" /> Inventory Tracking</div>
        <h1>Sign in</h1>
        {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
        <Field id="email" label="Email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" value={values.email} onChange={set('email')} aria-invalid={Boolean(errors.email)} />
        </Field>
        <Field id="password" label="Password" error={errors.password}>
          <input id="password" type="password" autoComplete="current-password" value={values.password} onChange={set('password')} aria-invalid={Boolean(errors.password)} />
        </Field>
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <p className="muted center">No account? <Link to="/register">Create one</Link></p>
      </form>
    </div>
  );
}
