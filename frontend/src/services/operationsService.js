import { api, toQuery } from './api';

export const pressingService = {
  list: (params) => api.get(`/pressings${toQuery(params)}`),
  get: (id) => api.get(`/pressings/${id}`),
  receive: (body) => api.post('/pressings', body),
  updateTransport: (id, body) => api.put(`/pressings/${id}/transport`, body),
  updateDate: (id, dateReception) => api.put(`/pressings/${id}/date`, { dateReception }),
  start: (id) => api.post(`/pressings/${id}/start`),
  finish: (id, body) => api.post(`/pressings/${id}/finish`, body),
  returnOil: (id, body) => api.post(`/pressings/${id}/return`, body),
  pay: (id, body) => api.post(`/pressings/${id}/pay`, body),
  paySeller: (id, body) => api.post(`/pressings/${id}/pay-seller`, body),
  cancel: (id, motif) => api.post(`/pressings/${id}/cancel`, { motif }),
  remove: (id) => api.delete(`/pressings/${id}`),
  accounts: () => api.get('/pressings/accounts'),
  oilAccount: (clientId) => api.get(`/pressings/clients/${clientId}/oil`),
  returnToClient: (clientId, body) => api.post(`/pressings/clients/${clientId}/return`, body),
  sellerAccount: (clientId) => api.get(`/pressings/clients/${clientId}/seller`),
  giveAdvance: (clientId, body) => api.post(`/pressings/clients/${clientId}/advances`, body),
};

export const productionService = {
  list: (params) => api.get(`/production${toQuery(params)}`),
  get: (id) => api.get(`/production/${id}`),
  save: (id, body) => (id ? api.put(`/production/${id}`, body) : api.post('/production', body)),
  start: (id) => api.post(`/production/${id}/start`),
  finish: (id, body) => api.post(`/production/${id}/finish`, body),
  cancel: (id, motif) => api.post(`/production/${id}/cancel`, { motif }),
};

export const traceService = {
  lot: (id) => api.get(`/traceability/lots/${id}`),
};
