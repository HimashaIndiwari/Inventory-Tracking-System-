import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { stockService } from '../services/services';
import { EmptyState, ErrorMessage, LoadingState, Pagination } from '../components/ui';
import TransactionTable from '../components/TransactionTable';

const PAGE_SIZE = 20;

/**
 * Movements for one product (route /products/:id/history). The product name comes from the movement rows, not a
 * separate product request, so the page still works for archived products, which GET /api/products/{id} hides.
 */
export default function ProductHistory() {
  const { id } = useParams();
  const productId = Number(id);
  const validId = Number.isInteger(productId) && productId > 0;

  // Filters belong to one product. Filters chosen for another product are ignored, so the page starts unfiltered.
  const [state, setState] = useState({ forProduct: productId, type: '', page: 0 });
  const filters = state.forProduct === productId ? state : { forProduct: productId, type: '', page: 0 };

  const history = useApi(
    () => (validId
      ? stockService.history({ productId, type: filters.type, page: filters.page, size: PAGE_SIZE })
      : Promise.resolve(null)),
    [productId, validId, filters.type, filters.page],
  );
  const page = history.data;
  const product = page?.content[0]?.product;

  if (!validId) {
    return (
      <div className="page">
        <EmptyState title="Invalid product link" message="Go back to the products list and choose a product." />
        <p><Link to="/products">Back to products</Link></p>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>{product ? `Stock history — ${product.name}` : 'Stock history'}</h1>
      </div>
      <p className="muted">
        {product && <><code>{product.sku}</code> · </>}
        <Link to="/products">Back to products</Link>
      </p>

      <div className="card filter-bar">
        <label htmlFor="product-history-type" className="sr-only">Type</label>
        <select
          id="product-history-type"
          value={filters.type}
          onChange={(e) => setState({ forProduct: productId, type: e.target.value, page: 0 })}
        >
          <option value="">All types</option>
          <option value="IN">Stock In</option>
          <option value="OUT">Stock Out</option>
          <option value="ADJUST">Adjustment</option>
        </select>
      </div>

      <section className="card">
        {history.loading && !page && <LoadingState />}
        <ErrorMessage error={history.error} onRetry={history.reload} />
        {page && page.content.length === 0 && (
          <EmptyState title="No movements for this product yet" message="Stock movements for this product will appear here." />
        )}
        {page && page.content.length > 0 && (
          <>
            <TransactionTable transactions={page.content} refreshing={history.loading} />
            <Pagination
              page={page.page}
              totalPages={page.totalPages}
              totalElements={page.totalElements}
              onPage={(n) => setState({ ...filters, page: n })}
            />
          </>
        )}
      </section>
    </div>
  );
}
