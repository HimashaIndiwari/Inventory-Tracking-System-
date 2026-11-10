import { useEffect, useState } from 'react';
import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useApi } from '../hooks/useApi';
import { productService, stockService } from '../services/services';
import { EmptyState, ErrorMessage, Field, LoadingState } from '../components/ui';
import { validateAdjustment, validateStock } from '../utils/validation';
import { formatQuantity } from '../utils/format';

const EMPTY = { productId: '', quantity: '', note: '' };

function buttonLabel(type) {
  if (type === 'ADJUST') return 'Record adjustment';
  return `Record Stock ${type === 'IN' ? 'In' : 'Out'}`;
}

export default function StockManagement() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const [type, setType] = useState('IN');
  const [productSearch, setProductSearch] = useState('');
  const [query, setQuery] = useState('');
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setQuery(productSearch), 300);
    return () => clearTimeout(t);
  }, [productSearch]);

  // Only active products are returned by the API, so archived products can never be selected.
  const products = useApi(() => productService.search({ search: query, page: 0, size: 100 }), [query]);
  const list = products.data?.content ?? [];
  const selected = list.find((p) => String(p.id) === values.productId);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const adjusting = type === 'ADJUST';
    const found = adjusting ? validateAdjustment(values) : validateStock(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      let tx;
      if (adjusting) {
        // Signed change: positive adds stock, negative removes it. The reason travels in the note field.
        tx = await stockService.adjust({ productId: Number(values.productId), delta: Number(values.quantity), note: values.note.trim() });
      } else {
        const body = { productId: Number(values.productId), quantity: Number(values.quantity), note: values.note || null };
        tx = type === 'IN' ? await stockService.stockIn(body) : await stockService.stockOut(body);
      }
      const verb = adjusting ? 'Adjustment recorded' : `Stock ${tx.type} recorded`;
      showToast(`${verb} for ${tx.product.name}. New quantity: ${formatQuantity(tx.quantityAfter)}.`);
      setValues((v) => ({ ...EMPTY, productId: v.productId }));
      products.reload();
    } catch (err) {
      const fieldErrors = err.fieldErrors ?? {};
      // The backend names the signed field "delta"; the form shows it on the "Change" input.
      setErrors(adjusting && fieldErrors.delta ? { ...fieldErrors, quantity: fieldErrors.delta } : fieldErrors);
      setFormError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header"><h1>Stock In / Out</h1></div>
      <div className="tabs" role="tablist" aria-label="Movement type">
        {[
          ['IN', 'Stock In', ArrowDownToLine],
          ['OUT', 'Stock Out', ArrowUpFromLine],
          ...(isAdmin ? [['ADJUST', 'Adjust', ArrowLeftRight]] : []),
        ].map(([key, label, Icon]) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={type === key}
            className={`tab ${type === key ? 'active' : ''}`}
            onClick={() => {
              // Each movement type starts with an empty amount and note; the chosen product is kept.
              setType(key);
              setValues((v) => ({ ...EMPTY, productId: v.productId }));
              setFormError(null);
              setErrors({});
            }}
          >
            <Icon size={16} aria-hidden="true" /> {label}
          </button>
        ))}
      </div>

      <form className="card form-card" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={formError} />
        <Field id="product-filter" label="Find product" hint="Filter the list below by name or SKU.">
          <input id="product-filter" value={productSearch} onChange={(e) => setProductSearch(e.target.value)} placeholder="Search by name or SKU" />
        </Field>
        {products.loading && !products.data && <LoadingState rows={1} />}
        <ErrorMessage error={products.error} onRetry={products.reload} />
        {products.data && list.length === 0 && <EmptyState title="No active products found" />}
        {list.length > 0 && (
          <Field id="productId" label="Product" error={errors.productId}>
            <select id="productId" value={values.productId} onChange={set('productId')}>
              <option value="">Select a product</option>
              {list.map((p) => (
                <option key={p.id} value={p.id}>{p.name} ({p.sku}) — {formatQuantity(p.quantity)} in stock</option>
              ))}
            </select>
          </Field>
        )}
        {products.data && products.data.totalElements > list.length && (
          <p className="muted small">Showing the first {list.length} of {products.data.totalElements} products. Use the search to narrow the list.</p>
        )}
        {selected && (
          <p className="stock-current">Current quantity: <strong>{formatQuantity(selected.quantity)}</strong></p>
        )}
        <div className="form-grid">
          {type === 'ADJUST' ? (
            <>
              <Field id="quantity" label="Change" error={errors.quantity} hint="Positive adds stock, negative removes it.">
                <input id="quantity" inputMode="numeric" value={values.quantity} onChange={set('quantity')} placeholder="e.g. 5 or -3" />
              </Field>
              <Field id="note" label="Reason (required)" error={errors.note}>
                <input id="note" value={values.note} onChange={set('note')} maxLength={255} placeholder="e.g. Stocktake found 3 extra" />
              </Field>
            </>
          ) : (
            <>
              <Field id="quantity" label="Quantity" error={errors.quantity} hint="Whole number from 1 to 1,000,000.">
                <input id="quantity" inputMode="numeric" value={values.quantity} onChange={set('quantity')} />
              </Field>
              <Field id="note" label="Note (optional)" error={errors.note}>
                <input id="note" value={values.note} onChange={set('note')} maxLength={255} placeholder={type === 'IN' ? 'e.g. Delivery from supplier' : 'e.g. Sold, written off'} />
              </Field>
            </>
          )}
        </div>
        <div className="form-actions">
          <button type="submit" className={`btn ${type === 'OUT' ? 'btn-danger' : 'btn-primary'}`} disabled={busy}>
            {busy ? 'Saving…' : buttonLabel(type)}
          </button>
        </div>
      </form>
    </div>
  );
}
