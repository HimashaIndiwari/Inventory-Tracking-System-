import { StatusBadge } from './ui';
import { formatDateTime, formatQuantity } from '../utils/format';

/** Badge colour per movement type. ADJUST is neutral: it can add or remove stock, so neither green nor red fits. */
export function movementTone(type) {
  if (type === 'IN') return 'success';
  if (type === 'OUT') return 'danger';
  return 'neutral';
}

/** IN and OUT quantities are sizes; an ADJUST quantity is signed, so a positive one gets an explicit plus sign. */
export function signedQuantity(t) {
  const digits = t.quantity.toLocaleString('en-US');
  return t.type === 'ADJUST' && t.quantity > 0 ? `+${digits}` : digits;
}

export default function TransactionTable({ transactions, refreshing = false }) {
  return (
    <table className={`table ${refreshing ? 'is-refreshing' : ''}`}>
      <thead><tr><th>Date</th><th>Product</th><th>Type</th><th>Quantity</th><th>After</th><th>User</th><th>Note</th></tr></thead>
      <tbody>
        {transactions.map((t) => (
          <tr key={t.id}>
            <td data-label="Date">{formatDateTime(t.createdAt)}</td>
            <td data-label="Product">{t.product.name}<div className="muted small">{t.product.sku}</div></td>
            <td data-label="Type"><StatusBadge tone={movementTone(t.type)}>{t.type}</StatusBadge></td>
            <td data-label="Quantity">{signedQuantity(t)}</td>
            <td data-label="After">{formatQuantity(t.quantityAfter)}</td>
            <td data-label="User">{t.user.displayName || t.user.email}</td>
            <td data-label="Note">{t.note || <span className="muted">—</span>}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
