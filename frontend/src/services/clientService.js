import { api, toQuery } from './api';

export const clientService = {
  list: (params) => api.get(`/clients${toQuery(params)}`),
  get: (id) => api.get(`/clients/${id}`),
  save: (id, body) => (id ? api.put(`/clients/${id}`, body) : api.post('/clients', body)),
  setStatus: (id, actif) => api.patch(`/clients/${id}/status`, { actif }),
};
