// Client-side checks mirror the backend limits (spec §6.4) for quick feedback.
// The backend remains authoritative; its fieldErrors are shown the same way.

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SKU = /^\s*[A-Za-z0-9-]{3,50}\s*$/;
const PRICE = /^\d{1,10}(\.\d{1,2})?$/;
const WHOLE = /^\d+$/;
const SIGNED_WHOLE = /^[+-]?\d+$/;

function maxLength(errors, field, value, max, label) {
  if (value && value.length > max) errors[field] = `${label} must be at most ${max} characters.`;
}

export function validateLogin({ email, password }) {
  const errors = {};
  if (!EMAIL.test(email.trim())) errors.email = 'Enter a valid email address.';
  if (!password) errors.password = 'Password is required.';
  return errors;
}

export function validateRegister({ email, displayName, password, confirmPassword }) {
  const errors = {};
  if (!EMAIL.test(email.trim())) errors.email = 'Enter a valid email address.';
  const name = displayName.trim();
  if (!name) errors.displayName = 'Display name is required.';
  else if (name.length > 100) errors.displayName = 'Display name must be at most 100 characters.';
  if (password.length < 8) errors.password = 'Password must be at least 8 characters.';
  if (confirmPassword !== password) errors.confirmPassword = 'Passwords do not match.';
  return errors;
}

export function validateProduct(v) {
  const errors = {};
  if (!SKU.test(v.sku)) errors.sku = 'SKU must be 3-50 characters: letters, digits and hyphens.';
  if (!v.name.trim()) errors.name = 'Name is required.';
  maxLength(errors, 'name', v.name.trim(), 150, 'Name');
  maxLength(errors, 'description', v.description, 500, 'Description');
  if (!v.categoryId) errors.categoryId = 'Category is required.';
  if (!PRICE.test(String(v.price).trim())) errors.price = 'Price must be 0 or more with at most 2 decimals.';
  if (!WHOLE.test(String(v.reorderLevel).trim())) errors.reorderLevel = 'Reorder level must be a whole number of 0 or more.';
  return errors;
}

export function validateCategory({ name, description }) {
  const errors = {};
  if (!name.trim()) errors.name = 'Name is required.';
  maxLength(errors, 'name', name.trim(), 100, 'Name');
  maxLength(errors, 'description', description, 255, 'Description');
  return errors;
}

export function validateSupplier(v) {
  const errors = {};
  if (!v.name.trim()) errors.name = 'Name is required.';
  maxLength(errors, 'name', v.name.trim(), 150, 'Name');
  maxLength(errors, 'contactPerson', v.contactPerson, 100, 'Contact person');
  if (v.email && !EMAIL.test(v.email.trim())) errors.email = 'Enter a valid email address.';
  maxLength(errors, 'email', v.email, 255, 'Email');
  maxLength(errors, 'phone', v.phone, 30, 'Phone');
  maxLength(errors, 'address', v.address, 255, 'Address');
  return errors;
}

export function validateStock({ productId, quantity, note }) {
  const errors = {};
  if (!productId) errors.productId = 'Select a product.';
  const q = String(quantity).trim();
  if (!WHOLE.test(q) || Number(q) < 1 || Number(q) > 1_000_000) {
    errors.quantity = 'Quantity must be a whole number from 1 to 1,000,000.';
  }
  maxLength(errors, 'note', note, 255, 'Note');
  return errors;
}

/** Signed change: positive adds stock, negative removes it. The reason is required (stored in the note field). */
export function validateAdjustment({ productId, quantity, note }) {
  const errors = {};
  if (!productId) errors.productId = 'Select a product.';
  const q = String(quantity).trim();
  if (!SIGNED_WHOLE.test(q)) {
    errors.quantity = 'Enter a whole number: positive to add stock, negative to remove it.';
  } else if (Number(q) === 0) {
    errors.quantity = 'Change cannot be zero.';
  } else if (Math.abs(Number(q)) > 1_000_000) {
    errors.quantity = 'Change must be between -1,000,000 and 1,000,000.';
  }
  if (!String(note ?? '').trim()) errors.note = 'A reason is required.';
  maxLength(errors, 'note', note, 255, 'Note');
  return errors;
}
