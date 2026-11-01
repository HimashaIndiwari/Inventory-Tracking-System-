import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';

export default function Forbidden() {
  return (
    <div className="empty-state page-message">
      <ShieldAlert size={40} aria-hidden="true" />
      <h1>Access denied</h1>
      <p className="muted">This page is available to administrators only.</p>
      <Link className="btn btn-primary" to="/">Back to dashboard</Link>
    </div>
  );
}
