import { useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useApi } from '../hooks/useApi';
import { categoryService } from '../services/services';
import { ConfirmDialog, EmptyState, ErrorMessage, Field, LoadingState, Modal } from '../components/ui';
import { validateCategory } from '../utils/validation';

function CategoryForm({ initial, onClose, onSaved }) {
  const [values, setValues] = useState({ name: initial?.name ?? '', description: initial?.description ?? '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const found = validateCategory(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const body = { name: values.name, description: values.description || null };
      const saved = initial ? await categoryService.update(initial.id, body) : await categoryService.create(body);
      onSaved(saved, Boolean(initial));
    } catch (err) {
      setErrors(err.fieldErrors ?? {});
      setFormError(err);
      setBusy(false);
    }
  }

  return (
    <Modal
      title={initial ? 'Edit category' : 'Add category'}
      onClose={onClose}
      footer={(
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" form="category-form" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </>
      )}
    >
      <form id="category-form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={formError} />
        <Field id="category-name" label="Name" error={errors.name}>
          <input id="category-name" value={values.name} onChange={set('name')} maxLength={100} />
        </Field>
        <Field id="category-description" label="Description (optional)" error={errors.description}>
          <textarea id="category-description" rows={3} value={values.description} onChange={set('description')} maxLength={255} />
        </Field>
      </form>
    </Modal>
  );
}

export default function Categories() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const { data, loading, error, reload } = useApi(() => categoryService.list(), []);
  const [editing, setEditing] = useState(null); // null | 'new' | category
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  async function confirmDelete() {
    setBusy(true);
    try {
      await categoryService.remove(deleting.id);
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
        <h1>Categories</h1>
        {isAdmin && <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} aria-hidden="true" /> Add category</button>}
      </div>
      <section className="card">
        {loading && !data && <LoadingState />}
        <ErrorMessage error={error} onRetry={reload} />
        {data && data.length === 0 && <EmptyState title="No categories yet" message={isAdmin ? 'Add a category to start adding products.' : undefined} />}
        {data && data.length > 0 && (
          <table className="table">
            <thead><tr><th>Name</th><th>Description</th><th>Products</th>{isAdmin && <th><span className="sr-only">Actions</span></th>}</tr></thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id}>
                  <td data-label="Name"><strong>{c.name}</strong></td>
                  <td data-label="Description">{c.description || <span className="muted">—</span>}</td>
                  <td data-label="Products" title="Includes archived products">{c.productCount}</td>
                  {isAdmin && (
                    <td className="actions">
                      <button type="button" className="btn btn-ghost btn-icon" aria-label={`Edit ${c.name}`} onClick={() => setEditing(c)}><Pencil size={16} /></button>
                      <button type="button" className="btn btn-ghost btn-icon danger" aria-label={`Delete ${c.name}`} onClick={() => setDeleting(c)}><Trash2 size={16} /></button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editing && (
        <CategoryForm
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
          title="Delete category"
          message={`Delete ${deleting.name}? Categories used by any product (including archived ones) cannot be deleted.`}
          confirmLabel="Delete"
          busy={busy}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
