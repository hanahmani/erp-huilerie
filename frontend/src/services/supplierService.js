import { api, toQuery } from './api';

export const supplierService = {
  list: (params) => api.get(`/suppliers${toQuery(params)}`),
  save: (id, body) => (id ? api.put(`/suppliers/${id}`, body) : api.post('/suppliers', body)),
  setStatus: (id, actif) => api.patch(`/suppliers/${id}/status`, { actif }),
};
