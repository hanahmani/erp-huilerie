import { api, toQuery } from './api';

export const reportService = {
  sales: (from, to) => api.get(`/reports/sales${toQuery({ from, to })}`),
  purchases: (from, to) => api.get(`/reports/purchases${toQuery({ from, to })}`),
  stock: () => api.get('/reports/stock'),
  finance: (from, to) => api.get(`/reports/finance${toQuery({ from, to })}`),
};

reportService.production = (from, to) => api.get(`/reports/production${toQuery({ from, to })}`);
