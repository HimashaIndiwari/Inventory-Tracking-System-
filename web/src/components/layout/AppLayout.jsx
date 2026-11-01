import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  ArrowLeftRight, Boxes, History, LayoutDashboard, LogOut, Menu, Package, Tags, Truck, X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/products', label: 'Products', icon: Package },
  { to: '/categories', label: 'Categories', icon: Tags },
  { to: '/suppliers', label: 'Suppliers', icon: Truck },
  { to: '/stock', label: 'Stock In / Out', icon: ArrowLeftRight },
  { to: '/transactions', label: 'History', icon: History },
];

function VerificationBanner() {
  const { user, emailVerified, refreshVerification } = useAuth();
  const [dismissed, setDismissed] = useState(false);
  const [checking, setChecking] = useState(false);

  // FR-AUTH-11: re-check when the window regains focus (the user may have clicked the email link).
  useEffect(() => {
    if (!user || emailVerified) return undefined;
    const onFocus = () => refreshVerification().catch(() => {});
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [user, emailVerified, refreshVerification]);

  if (!user || emailVerified || dismissed) return null;
  return (
    <div className="banner" role="status">
      <span>Please verify your email address. A verification link was sent to {user.email}.</span>
      <div className="banner-actions">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={checking}
          onClick={async () => {
            setChecking(true);
            try { await refreshVerification(); } finally { setChecking(false); }
          }}
        >
          {checking ? 'Checking…' : "I've verified my email"}
        </button>
        <button type="button" className="btn btn-ghost btn-icon" aria-label="Dismiss" onClick={() => setDismissed(true)}>
          <X size={16} />
        </button>
      </div>
    </div>
  );
}

export default function AppLayout() {
  const { profile, logout } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const location = useLocation();

  useEffect(() => setDrawerOpen(false), [location.pathname]);

  useEffect(() => {
    if (!drawerOpen) return undefined;
    const onKey = (e) => e.key === 'Escape' && setDrawerOpen(false);
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [drawerOpen]);

  return (
    <div className="app-shell">
      <aside className={`sidebar ${drawerOpen ? 'open' : ''}`} aria-label="Main navigation">
        <div className="sidebar-brand">
          <Boxes size={24} aria-hidden="true" />
          <span className="nav-label">Inventory</span>
          <button type="button" className="btn btn-ghost btn-icon drawer-close" aria-label="Close menu" onClick={() => setDrawerOpen(false)}>
            <X size={18} />
          </button>
        </div>
        <nav>
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className="nav-item" title={label}>
              <Icon size={20} aria-hidden="true" />
              <span className="nav-label">{label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
      {drawerOpen && <div className="drawer-backdrop" onClick={() => setDrawerOpen(false)} aria-hidden="true" />}

      <div className="main-area">
        <header className="navbar">
          <button type="button" className="btn btn-ghost btn-icon menu-button" aria-label="Open menu" onClick={() => setDrawerOpen(true)}>
            <Menu size={20} />
          </button>
          <div className="navbar-user">
            <span className="user-name">{profile?.displayName || profile?.email}</span>
            <span className={`badge ${profile?.role === 'ADMIN' ? 'badge-primary' : 'badge-neutral'}`}>{profile?.role}</span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
              <LogOut size={16} aria-hidden="true" /> <span className="hide-mobile">Sign out</span>
            </button>
          </div>
        </header>
        <VerificationBanner />
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
