import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorMessage, LoadingState } from './ui';
import Forbidden from '../pages/Forbidden';

/** UX only: the backend enforces every permission (FR-AUTH-13). */
export default function ProtectedRoute({ adminOnly = false, children }) {
  const { user, profile, loading, error, retryProfile, logout } = useAuth();
  const location = useLocation();

  if (loading) return <div className="page-center"><LoadingState label="Restoring your session…" /></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (!profile) {
    if (!error) return <div className="page-center"><LoadingState label="Loading your profile…" /></div>;
    return (
      <div className="page-center">
        <div className="card auth-card">
          <h1>We could not load your account</h1>
          <ErrorMessage error={error} onRetry={retryProfile} />
          <button type="button" className="btn btn-ghost" onClick={logout}>Sign out</button>
        </div>
      </div>
    );
  }
  if (adminOnly && profile.role !== 'ADMIN') return <Forbidden />;
  return children ?? <Outlet />;
}
