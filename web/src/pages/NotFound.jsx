import { Link } from 'react-router-dom';
import { SearchX } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="empty-state page-message">
      <SearchX size={40} aria-hidden="true" />
      <h1>Page not found</h1>
      <p className="muted">The page you are looking for does not exist.</p>
      <Link className="btn btn-primary" to="/">Back to dashboard</Link>
    </div>
  );
}
