import { api, toQuery } from './api';

export const salesService = {
  catalog: () => api.get('/sales/catalog'),
  quickSale: (body) => api.post('/sales/quick', body),
  list: (params) => api.get(`/sales${toQuery(params)}`),
  get: (id) => api.get(`/sales/${id}`),
  cashiers: () => api.get('/sales/cashiers'),
  recordPrint: (id) => api.post(`/sales/${id}/print`),
  cancel: (id, motif) => api.post(`/sales/${id}/cancel`, { motif }),
};

export const creditService = {
  receivables: () => api.get('/sales/receivables'),
  openCredits: (clientId) => api.get(`/sales/receivables/${clientId}`),
  settle: (saleId, body) => api.post(`/sales/${saleId}/settle`, body),
};
