import { apiRequest } from './apiClient';

// One small module per backend resource (spec §6.2).

export const authService = {
  me: () => apiRequest('/api/auth/me'),
};

export const dashboardService = {
  get: () => apiRequest('/api/dashboard'),
};

export const productService = {
  search: (params) => apiRequest('/api/products', { params }),
  get: (id) => apiRequest(`/api/products/${id}`),
  create: (body) => apiRequest('/api/products', { method: 'POST', body }),
  update: (id, body) => apiRequest(`/api/products/${id}`, { method: 'PUT', body }),
  archive: (id) => apiRequest(`/api/products/${id}`, { method: 'DELETE' }),
};

function crud(base) {
  return {
    list: () => apiRequest(base),
    create: (body) => apiRequest(base, { method: 'POST', body }),
    update: (id, body) => apiRequest(`${base}/${id}`, { method: 'PUT', body }),
    remove: (id) => apiRequest(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export const categoryService = crud('/api/categories');
export const supplierService = crud('/api/suppliers');

export const stockService = {
  stockIn: (body) => apiRequest('/api/stock/in', { method: 'POST', body }),
  stockOut: (body) => apiRequest('/api/stock/out', { method: 'POST', body }),
  history: (params) => apiRequest('/api/stock/history', { params }),
};
