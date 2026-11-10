import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { useApi } from '../hooks/useApi';
import { categoryService, productService, supplierService } from '../services/services';
import { ErrorMessage, Field, LoadingState } from '../components/ui';
import { validateProduct } from '../utils/validation';
import { formatQuantity } from '../utils/format';

const EMPTY = { sku: '', name: '', description: '', categoryId: '', supplierId: '', price: '', reorderLevel: '0' };

export default function ProductForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [busy, setBusy] = useState(false);

  const lookups = useApi(() => Promise.all([categoryService.list(), supplierService.list()]), []);
  const existing = useApi(() => (editing ? productService.get(id) : Promise.resolve(null)), [id]);

  useEffect(() => {
    const p = existing.data;
    if (p) {
      setValues({
        sku: p.sku, name: p.name, description: p.description ?? '', categoryId: String(p.category.id),
        supplierId: p.supplier ? String(p.supplier.id) : '', price: String(p.price), reorderLevel: String(p.reorderLevel),
      });
    }
  }, [existing.data]);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const found = validateProduct(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length) return;
    const body = {
      sku: values.sku,
      name: values.name,
      description: values.description || null,
      categoryId: Number(values.categoryId),
      supplierId: values.supplierId ? Number(values.supplierId) : null,
      // ≤ 12 significant digits (DECIMAL(12,2)), so a JS number round-trips exactly
      price: Number(values.price.trim()),
      reorderLevel: Number(values.reorderLevel),
    };
    setBusy(true);
    try {
      const saved = editing ? await productService.update(id, body) : await productService.create(body);
      showToast(editing ? `${saved.name} was updated.` : `${saved.name} was created with quantity 0.`);
      navigate('/products');
    } catch (err) {
      setErrors(err.fieldErrors ?? {});
      setFormError(err);
      setBusy(false);
    }
  }

  if (lookups.loading || existing.loading) return <div className="page"><LoadingState /></div>;
  if (lookups.error || existing.error) {
    return (
      <div className="page">
        <ErrorMessage error={lookups.error || existing.error} onRetry={() => { lookups.reload(); existing.reload(); }} />
        <Link to="/products">Back to products</Link>
      </div>
    );
  }
  const [categories, suppliers] = lookups.data;

  return (
    <div className="page">
      <div className="page-header"><h1>{editing ? 'Edit product' : 'Add product'}</h1></div>
      <form className="card form-card" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={formError} />
        {categories.length === 0 && (
          <div className="alert alert-warning">Create a category first — every product needs one. <Link to="/categories">Go to categories</Link></div>
        )}
        <div className="form-grid">
          <Field id="sku" label="SKU" error={errors.sku} hint="3-50 letters, digits or hyphens; stored in uppercase.">
            <input id="sku" value={values.sku} onChange={set('sku')} maxLength={60} />
          </Field>
          <Field id="name" label="Name" error={errors.name}>
            <input id="name" value={values.name} onChange={set('name')} maxLength={150} />
          </Field>
          <Field id="categoryId" label="Category" error={errors.categoryId}>
            <select id="categoryId" value={values.categoryId} onChange={set('categoryId')}>
              <option value="">Select a category</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field id="supplierId" label="Supplier (optional)" error={errors.supplierId}>
            <select id="supplierId" value={values.supplierId} onChange={set('supplierId')}>
              <option value="">No supplier</option>
              {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field id="price" label="Price" error={errors.price}>
            <input id="price" inputMode="decimal" value={values.price} onChange={set('price')} placeholder="0.00" />
          </Field>
          <Field id="reorderLevel" label="Reorder level" error={errors.reorderLevel} hint="Low stock when quantity is at or below this level.">
            <input id="reorderLevel" inputMode="numeric" value={values.reorderLevel} onChange={set('reorderLevel')} />
          </Field>
        </div>
        <Field id="description" label="Description (optional)" error={errors.description}>
          <textarea id="description" rows={3} value={values.description} onChange={set('description')} maxLength={500} />
        </Field>
        <p className="muted small">
          {editing
            ? `Current quantity: ${formatQuantity(existing.data.quantity)}. Quantity changes only through Stock In, Stock Out or Adjust.`
            : 'New products start with quantity 0. Use Stock In to add stock.'}
        </p>
        <div className="form-actions">
          <Link className="btn btn-secondary" to="/products">Cancel</Link>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save product'}</button>
        </div>
      </form>
    </div>
  );
}
