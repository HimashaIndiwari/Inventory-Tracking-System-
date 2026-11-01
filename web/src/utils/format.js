// Quantities arrive as decimal strings (BIGINT-backed, may exceed 2^53). They are formatted and compared with
// BigInt and never converted with Number()/parseInt(), except for the final small bar-width ratio.

const quantityFormatter = new Intl.NumberFormat('en-US');
const priceFormatter = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function formatQuantity(value) {
  if (value === null || value === undefined || value === '') return '—';
  return quantityFormatter.format(BigInt(value));
}

export function maxQuantity(values) {
  return values.reduce((max, v) => (BigInt(v) > BigInt(max) ? String(v) : max), '0');
}

/** Width in percent (0-100, 0.1 % resolution). max "0" means every bar is empty; no division happens. */
export function barWidthPercent(part, max) {
  const maxValue = BigInt(max);
  if (maxValue === 0n) return 0;
  return Number((BigInt(part) * 1000n) / maxValue) / 10;
}

export function formatPrice(value) {
  return priceFormatter.format(Number(value));
}

/** API instants are ISO-8601 UTC; shown in the viewer's local time. */
export function formatDateTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

/**
 * Converts local calendar dates (yyyy-mm-dd) into the API's UTC instants.
 * "from" is the start of that day (inclusive); "to" is the start of the following day (exclusive),
 * so choosing the same day for both returns that whole day.
 */
export function dateRangeToInstants(fromDate, toDate) {
  const from = fromDate ? new Date(`${fromDate}T00:00:00`).toISOString() : undefined;
  let to;
  if (toDate) {
    const end = new Date(`${toDate}T00:00:00`);
    end.setDate(end.getDate() + 1);
    to = end.toISOString();
  }
  return { from, to };
}
