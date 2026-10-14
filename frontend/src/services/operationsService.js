import { api, toQuery } from './api';

export const pressingService = {
  list: (params) => api.get(`/pressings${toQuery(params)}`),
  get: (id) => api.get(`/pressings/${id}`),
  receive: (body) => api.post('/pressings', body),
  start: (id) => api.post(`/pressings/${id}/start`),
  finish: (id, body) => api.post(`/pressings/${id}/finish`, body),
  returnOil: (id, body) => api.post(`/pressings/${id}/return`, body),
  pay: (id, body) => api.post(`/pressings/${id}/pay`, body),
  cancel: (id, motif) => api.post(`/pressings/${id}/cancel`, { motif }),
};

export const productionService = {
  list: (params) => api.get(`/production${toQuery(params)}`),
  get: (id) => api.get(`/production/${id}`),
  save: (id, body) => (id ? api.put(`/production/${id}`, body) : api.post('/production', body)),
  start: (id) => api.post(`/production/${id}/start`),
  finish: (id, body) => api.post(`/production/${id}/finish`, body),
  cancel: (id, motif) => api.post(`/production/${id}/cancel`, { motif }),
};

export const packagingService = {
  list: (params) => api.get(`/packaging${toQuery(params)}`),
  create: (body) => api.post('/packaging', body),
  cancel: (id, motif) => api.post(`/packaging/${id}/cancel`, { motif }),
};

export const traceService = {
  lot: (id) => api.get(`/traceability/lots/${id}`),
};
