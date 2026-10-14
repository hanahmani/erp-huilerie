import { api, toQuery } from './api';

export const purchaseService = {
  list: (params) => api.get(`/purchases${toQuery(params)}`),
  get: (id) => api.get(`/purchases/${id}`),
  save: (id, body) => (id ? api.put(`/purchases/${id}`, body) : api.post('/purchases', body)),
  validate: (id) => api.post(`/purchases/${id}/validate`),
  cancel: (id, motif) => api.post(`/purchases/${id}/cancel`, { motif }),
  invoiceRef: (id, refFacture) => api.patch(`/purchases/${id}/invoice-ref`, { refFacture }),
  receive: (id, body) => api.post(`/purchases/${id}/receive`, body),
  pay: (id, body) => api.post(`/purchases/${id}/pay`, body),
  payables: () => api.get('/purchases/payables'),
};
