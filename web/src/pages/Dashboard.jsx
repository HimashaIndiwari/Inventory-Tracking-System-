import { Link } from 'react-router-dom';
import { AlertTriangle, Boxes, Package } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import { dashboardService } from '../services/services';
import { EmptyState, ErrorMessage, LoadingState, StatusBadge } from '../components/ui';
import { barWidthPercent, formatDateTime, formatQuantity, maxQuantity } from '../utils/format';

function DashboardCard({ icon: Icon, label, value, tone }) {
  return (
    <div className="card stat-card">
      <div className={`stat-icon stat-${tone}`}><Icon size={22} aria-hidden="true" /></div>
      <div>
        <p className="muted">{label}</p>
        <p className="stat-value">{value}</p>
      </div>
    </div>
  );
}

export function StockByCategoryChart({ rows }) {
  if (rows.length === 0) return <EmptyState title="No categories yet" />;
  const max = maxQuantity(rows.map((r) => r.totalQuantity));
  return (
    <ul className="bar-chart">
      {rows.map((row) => (
        <li key={row.categoryName}>
          <div className="bar-label">
            <span>{row.categoryName}</span>
            <span className="muted">{formatQuantity(row.totalQuantity)}</span>
          </div>
          <div className="bar-track" aria-hidden="true">
            <div className="bar-fill" style={{ width: `${barWidthPercent(row.totalQuantity, max)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

export default function Dashboard() {
  const { data, loading, error, reload } = useApi(() => dashboardService.get(), []);

  return (
    <div className="page">
      <div className="page-header"><h1>Dashboard</h1></div>
      {loading && !data && <LoadingState />}
      <ErrorMessage error={error} onRetry={reload} />
      {data && (
        <>
          <div className="stat-grid">
            <DashboardCard icon={Package} label="Active products" value={data.totalProducts.toLocaleString('en-US')} tone="primary" />
            <DashboardCard icon={Boxes} label="Total stock quantity" value={formatQuantity(data.totalStockQuantity)} tone="teal" />
            <DashboardCard icon={AlertTriangle} label="Low-stock products" value={data.lowStockCount.toLocaleString('en-US')} tone="warning" />
          </div>

          <div className="dashboard-grid">
            <section className="card">
              <h2>Low stock</h2>
              {data.lowStockProducts.length === 0 ? (
                <EmptyState title="No low-stock products" message="Every active product is above its reorder level." />
              ) : (
                <table className="table">
                  <thead><tr><th>Product</th><th>Quantity</th><th>Reorder level</th></tr></thead>
                  <tbody>
                    {data.lowStockProducts.map((p) => (
                      <tr key={p.id}>
                        <td data-label="Product"><strong>{p.name}</strong><div className="muted small">{p.sku}</div></td>
                        <td data-label="Quantity"><StatusBadge tone="warning">{formatQuantity(p.quantity)}</StatusBadge></td>
                        <td data-label="Reorder level">{p.reorderLevel.toLocaleString('en-US')}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>

            <section className="card">
              <h2>Stock by category</h2>
              <StockByCategoryChart rows={data.stockByCategory} />
            </section>
          </div>

          <section className="card">
            <div className="section-header">
              <h2>Recent movements</h2>
              <Link to="/transactions">View all</Link>
            </div>
            {data.recentTransactions.length === 0 ? (
              <EmptyState title="No stock movements yet" message="Record a Stock IN to get started." />
            ) : (
              <table className="table">
                <thead><tr><th>Date</th><th>Product</th><th>Type</th><th>Quantity</th><th>After</th><th>User</th></tr></thead>
                <tbody>
                  {data.recentTransactions.map((t) => (
                    <tr key={t.id}>
                      <td data-label="Date">{formatDateTime(t.createdAt)}</td>
                      <td data-label="Product">{t.product.name}<div className="muted small">{t.product.sku}</div></td>
                      <td data-label="Type"><StatusBadge tone={t.type === 'IN' ? 'success' : 'danger'}>{t.type}</StatusBadge></td>
                      <td data-label="Quantity">{t.quantity.toLocaleString('en-US')}</td>
                      <td data-label="After">{formatQuantity(t.quantityAfter)}</td>
                      <td data-label="User">{t.user.displayName || t.user.email}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </div>
  );
}
