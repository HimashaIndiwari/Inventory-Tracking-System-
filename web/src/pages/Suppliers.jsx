import { useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useApi } from '../hooks/useApi';
import { supplierService } from '../services/services';
import { ConfirmDialog, EmptyState, ErrorMessage, Field, LoadingState, Modal } from '../components/ui';
import { validateSupplier } from '../utils/validation';

const FIELDS = [
  { key: 'name', label: 'Name', max: 150 },
  { key: 'contactPerson', label: 'Contact person (optional)', max: 100 },
  { key: 'email', label: 'Email (optional)', max: 255, type: 'email' },
  { key: 'phone', label: 'Phone (optional)', max: 30, type: 'tel' },
  { key: 'address', label: 'Address (optional)', max: 255 },
];

function SupplierForm({ initial, onClose, onSaved }) {
  const [values, setValues] = useState(Object.fromEntries(FIELDS.map(({ key }) => [key, initial?.[key] ?? ''])));
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    const found = validateSupplier(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const body = Object.fromEntries(Object.entries(values).map(([k, v]) => [k, v.trim() === '' ? null : v]));
      const saved = initial ? await supplierService.update(initial.id, body) : await supplierService.create(body);
      onSaved(saved, Boolean(initial));
    } catch (err) {
      setErrors(err.fieldErrors ?? {});
      setFormError(err);
      setBusy(false);
    }
  }

  return (
    <Modal
      title={initial ? 'Edit supplier' : 'Add supplier'}
      onClose={onClose}
      footer={(
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" form="supplier-form" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </>
      )}
    >
      <form id="supplier-form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={formError} />
        {FIELDS.map(({ key, label, max, type }) => (
          <Field key={key} id={`supplier-${key}`} label={label} error={errors[key]}>
            <input
              id={`supplier-${key}`}
              type={type ?? 'text'}
              value={values[key]}
              maxLength={max}
              onChange={(e) => setValues((v) => ({ ...v, [key]: e.target.value }))}
            />
          </Field>
        ))}
      </form>
    </Modal>
  );
}

export default function Suppliers() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const { data, loading, error, reload } = useApi(() => supplierService.list(), []);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  async function confirmDelete() {
    setBusy(true);
    try {
      await supplierService.remove(deleting.id);
      showToast(`${deleting.name} was deleted.`);
      reload();
    } catch (err) {
      showToast(err.message, 'danger');
    } finally {
      setBusy(false);
      setDeleting(null);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Suppliers</h1>
        {isAdmin && <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} aria-hidden="true" /> Add supplier</button>}
      </div>
      <section className="card">
        {loading && !data && <LoadingState />}
        <ErrorMessage error={error} onRetry={reload} />
        {data && data.length === 0 && <EmptyState title="No suppliers yet" />}
        {data && data.length > 0 && (
          <table className="table">
            <thead><tr><th>Name</th><th>Contact</th><th>Email</th><th>Phone</th><th>Products</th>{isAdmin && <th><span className="sr-only">Actions</span></th>}</tr></thead>
            <tbody>
              {data.map((s) => (
                <tr key={s.id}>
                  <td data-label="Name"><strong>{s.name}</strong>{s.address && <div className="muted small">{s.address}</div>}</td>
                  <td data-label="Contact">{s.contactPerson || <span className="muted">—</span>}</td>
                  <td data-label="Email">{s.email ? <a href={`mailto:${s.email}`}>{s.email}</a> : <span className="muted">—</span>}</td>
                  <td data-label="Phone">{s.phone || <span className="muted">—</span>}</td>
                  <td data-label="Products" title="Includes archived products">{s.productCount}</td>
                  {isAdmin && (
                    <td className="actions">
                      <button type="button" className="btn btn-ghost btn-icon" aria-label={`Edit ${s.name}`} onClick={() => setEditing(s)}><Pencil size={16} /></button>
                      <button type="button" className="btn btn-ghost btn-icon danger" aria-label={`Delete ${s.name}`} onClick={() => setDeleting(s)}><Trash2 size={16} /></button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editing && (
        <SupplierForm
          initial={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(saved, wasEdit) => {
            showToast(`${saved.name} was ${wasEdit ? 'updated' : 'created'}.`);
            setEditing(null);
            reload();
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete supplier"
          message={`Delete ${deleting.name}? Suppliers used by any product (including archived ones) cannot be deleted.`}
          confirmLabel="Delete"
          busy={busy}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
