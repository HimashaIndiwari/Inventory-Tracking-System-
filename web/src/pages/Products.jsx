import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Archive, Pencil, Plus, Search } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useApi } from '../hooks/useApi';
import { categoryService, productService } from '../services/services';
import { ConfirmDialog, EmptyState, ErrorMessage, LoadingState, Pagination, StatusBadge } from '../components/ui';
import { formatPrice, formatQuantity } from '../utils/format';

const PAGE_SIZE = 20;

export default function Products() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const [searchInput, setSearchInput] = useState('');
  const [filters, setFilters] = useState({ search: '', categoryId: '', lowStock: false, page: 0 });
  const [archiving, setArchiving] = useState(null);
  const [busy, setBusy] = useState(false);

  // Debounce the search box so every keystroke does not hit the API.
  useEffect(() => {
    const t = setTimeout(() => setFilters((f) => (f.search === searchInput ? f : { ...f, search: searchInput, page: 0 })), 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  const categories = useApi(() => categoryService.list(), []);
  const products = useApi(
    () => productService.search({ ...filters, lowStock: filters.lowStock || undefined, size: PAGE_SIZE }),
    [filters],
  );

  async function confirmArchive() {
    setBusy(true);
    try {
      await productService.archive(archiving.id);
      showToast(`${archiving.name} was archived.`);
      setArchiving(null);
      products.reload();
    } catch (err) {
      showToast(err.message, 'danger');
      setArchiving(null);
    } finally {
      setBusy(false);
    }
  }

  const page = products.data;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Products</h1>
        {isAdmin && <Link className="btn btn-primary" to="/products/new"><Plus size={16} aria-hidden="true" /> Add product</Link>}
      </div>

      <div className="card filter-bar">
        <div className="search-input">
          <Search size={16} aria-hidden="true" />
          <label htmlFor="product-search" className="sr-only">Search products</label>
          <input id="product-search" placeholder="Search by name or SKU" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
        </div>
        <label htmlFor="category-filter" className="sr-only">Category</label>
        <select id="category-filter" value={filters.categoryId} onChange={(e) => setFilters((f) => ({ ...f, categoryId: e.target.value, page: 0 }))}>
          <option value="">All categories</option>
          {(categories.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <label className="checkbox">
          <input type="checkbox" checked={filters.lowStock} onChange={(e) => setFilters((f) => ({ ...f, lowStock: e.target.checked, page: 0 }))} />
          Low stock only
        </label>
      </div>

      <section className="card">
        {products.loading && !page && <LoadingState />}
        <ErrorMessage error={products.error} onRetry={products.reload} />
        {page && page.content.length === 0 && (
          <EmptyState
            title="No products found"
            message={filters.search || filters.categoryId || filters.lowStock ? 'Try changing the search or filters.' : 'Products you add will appear here.'}
          />
        )}
        {page && page.content.length > 0 && (
          <>
            <table className={`table ${products.loading ? 'is-refreshing' : ''}`}>
              <thead>
                <tr><th>SKU</th><th>Name</th><th>Category</th><th>Supplier</th><th>Price</th><th>Quantity</th><th>Status</th>{isAdmin && <th><span className="sr-only">Actions</span></th>}</tr>
              </thead>
              <tbody>
                {page.content.map((p) => (
                  <tr key={p.id}>
                    <td data-label="SKU"><code>{p.sku}</code></td>
                    <td data-label="Name"><strong>{p.name}</strong></td>
                    <td data-label="Category">{p.category.name}</td>
                    <td data-label="Supplier">{p.supplier?.name ?? <span className="muted">—</span>}</td>
                    <td data-label="Price">{formatPrice(p.price)}</td>
                    <td data-label="Quantity">{formatQuantity(p.quantity)}</td>
                    <td data-label="Status">
                      {p.lowStock ? <StatusBadge tone="warning">Low stock</StatusBadge> : <StatusBadge tone="success">In stock</StatusBadge>}
                    </td>
                    {isAdmin && (
                      <td className="actions">
                        <Link className="btn btn-ghost btn-icon" to={`/products/${p.id}/edit`} aria-label={`Edit ${p.name}`}><Pencil size={16} /></Link>
                        <button type="button" className="btn btn-ghost btn-icon danger" aria-label={`Archive ${p.name}`} onClick={() => setArchiving(p)}>
                          <Archive size={16} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={page.page} totalPages={page.totalPages} totalElements={page.totalElements} onPage={(n) => setFilters((f) => ({ ...f, page: n }))} />
          </>
        )}
      </section>

      {archiving && (
        <ConfirmDialog
          title="Archive product"
          message={`Archive ${archiving.name} (${archiving.sku})? It will be hidden from lists and stock forms; its history is kept. Products with stock cannot be archived.`}
          confirmLabel="Archive"
          busy={busy}
          onConfirm={confirmArchive}
          onCancel={() => setArchiving(null)}
        />
      )}
    </div>
  );
}
