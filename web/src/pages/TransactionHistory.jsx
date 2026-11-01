import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import { stockService } from '../services/services';
import { EmptyState, ErrorMessage, LoadingState, Pagination, StatusBadge } from '../components/ui';
import { dateRangeToInstants, formatDateTime, formatQuantity } from '../utils/format';

const PAGE_SIZE = 20;

export default function TransactionHistory() {
  const [searchInput, setSearchInput] = useState('');
  const [filters, setFilters] = useState({ search: '', type: '', fromDate: '', toDate: '', page: 0 });
  const rangeError = filters.fromDate && filters.toDate && filters.fromDate > filters.toDate
    ? 'The "from" date must not be after the "to" date.'
    : null;

  useEffect(() => {
    const t = setTimeout(() => setFilters((f) => (f.search === searchInput ? f : { ...f, search: searchInput, page: 0 })), 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  const history = useApi(() => {
    if (rangeError) return Promise.resolve(null);
    const { from, to } = dateRangeToInstants(filters.fromDate, filters.toDate);
    return stockService.history({ search: filters.search, type: filters.type, from, to, page: filters.page, size: PAGE_SIZE });
  }, [filters]);

  const setFilter = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value, page: 0 }));
  const page = history.data;

  return (
    <div className="page">
      <div className="page-header"><h1>Transaction history</h1></div>
      <div className="card filter-bar">
        <div className="search-input">
          <Search size={16} aria-hidden="true" />
          <label htmlFor="history-search" className="sr-only">Search by product name or SKU</label>
          <input id="history-search" placeholder="Search by product name or SKU" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
        </div>
        <label htmlFor="type-filter" className="sr-only">Type</label>
        <select id="type-filter" value={filters.type} onChange={setFilter('type')}>
          <option value="">All types</option>
          <option value="IN">Stock In</option>
          <option value="OUT">Stock Out</option>
        </select>
        <label className="date-filter">From <input type="date" value={filters.fromDate} onChange={setFilter('fromDate')} /></label>
        <label className="date-filter">To <input type="date" value={filters.toDate} onChange={setFilter('toDate')} /></label>
      </div>

      <section className="card">
        {rangeError && <div className="alert alert-danger" role="alert">{rangeError}</div>}
        {!rangeError && history.loading && !page && <LoadingState />}
        <ErrorMessage error={history.error} onRetry={history.reload} />
        {page && page.content.length === 0 && <EmptyState title="No transactions found" message="Stock movements matching the filters will appear here." />}
        {page && page.content.length > 0 && (
          <>
            <table className={`table ${history.loading ? 'is-refreshing' : ''}`}>
              <thead><tr><th>Date</th><th>Product</th><th>Type</th><th>Quantity</th><th>After</th><th>User</th><th>Note</th></tr></thead>
              <tbody>
                {page.content.map((t) => (
                  <tr key={t.id}>
                    <td data-label="Date">{formatDateTime(t.createdAt)}</td>
                    <td data-label="Product">{t.product.name}<div className="muted small">{t.product.sku}</div></td>
                    <td data-label="Type"><StatusBadge tone={t.type === 'IN' ? 'success' : 'danger'}>{t.type}</StatusBadge></td>
                    <td data-label="Quantity">{t.quantity.toLocaleString('en-US')}</td>
                    <td data-label="After">{formatQuantity(t.quantityAfter)}</td>
                    <td data-label="User">{t.user.displayName || t.user.email}</td>
                    <td data-label="Note">{t.note || <span className="muted">—</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={page.page} totalPages={page.totalPages} totalElements={page.totalElements} onPage={(n) => setFilters((f) => ({ ...f, page: n }))} />
          </>
        )}
      </section>
    </div>
  );
}
