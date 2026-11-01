import { describe, expect, it } from 'vitest';
import { validateProduct, validateRegister, validateStock, validateSupplier } from './validation';

const product = { sku: 'elec-001', name: 'Mouse', description: '', categoryId: '1', supplierId: '', price: '24.99', reorderLevel: '5' };

describe('validateRegister', () => {
  const ok = { email: 'a@example.com', displayName: 'Ann', password: 'secret123', confirmPassword: 'secret123' };

  it('accepts valid input', () => {
    expect(validateRegister(ok)).toEqual({});
  });

  it('requires a display name of at most 100 characters', () => {
    expect(validateRegister({ ...ok, displayName: '   ' }).displayName).toBe('Display name is required.');
    expect(validateRegister({ ...ok, displayName: 'a'.repeat(101) }).displayName).toBe('Display name must be at most 100 characters.');
  });

  it('requires 8+ character matching passwords', () => {
    expect(validateRegister({ ...ok, password: 'short', confirmPassword: 'short' }).password).toBeDefined();
    expect(validateRegister({ ...ok, confirmPassword: 'different1' }).confirmPassword).toBe('Passwords do not match.');
  });
});

describe('validateProduct', () => {
  it('accepts a padded lowercase SKU (the backend normalises it)', () => {
    expect(validateProduct({ ...product, sku: ' elec-001 ' })).toEqual({});
  });

  it('rejects SKUs outside 3-50 letters, digits and hyphens', () => {
    expect(validateProduct({ ...product, sku: 'AB' }).sku).toBeDefined();
    expect(validateProduct({ ...product, sku: 'A'.repeat(51) }).sku).toBeDefined();
    expect(validateProduct({ ...product, sku: 'EL EC' }).sku).toBeDefined();
  });

  it('rejects negative prices and more than two decimals', () => {
    expect(validateProduct({ ...product, price: '-1' }).price).toBeDefined();
    expect(validateProduct({ ...product, price: '1.005' }).price).toBeDefined();
    expect(validateProduct({ ...product, price: '0' })).toEqual({});
  });

  it('requires a category and a non-negative whole reorder level', () => {
    expect(validateProduct({ ...product, categoryId: '' }).categoryId).toBeDefined();
    expect(validateProduct({ ...product, reorderLevel: '1.5' }).reorderLevel).toBeDefined();
  });
});

describe('validateStock', () => {
  it('requires a whole quantity from 1 to 1,000,000', () => {
    expect(validateStock({ productId: '1', quantity: '0', note: '' }).quantity).toBeDefined();
    expect(validateStock({ productId: '1', quantity: '1000001', note: '' }).quantity).toBeDefined();
    expect(validateStock({ productId: '1', quantity: '1000000', note: '' })).toEqual({});
  });

  it('limits notes to 255 characters', () => {
    expect(validateStock({ productId: '1', quantity: '1', note: 'n'.repeat(256) }).note).toBeDefined();
  });
});

describe('validateSupplier', () => {
  it('checks the email format only when provided', () => {
    expect(validateSupplier({ name: 'Acme', email: '' })).toEqual({});
    expect(validateSupplier({ name: 'Acme', email: 'not-an-email' }).email).toBeDefined();
  });
});
