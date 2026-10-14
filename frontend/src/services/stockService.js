import { api, toQuery } from './api';

export const stockService = {
  state: (params) => api.get(`/stock/state${toQuery(params)}`),
  movements: (params) => api.get(`/stock/movements${toQuery(params)}`),
  createMovement: (body) => api.post('/stock/movements', body),
  lots: (params) => api.get(`/stock/lots${toQuery(params)}`),
  createLot: (body) => api.post('/stock/lots', body),
  setLotStatus: (id, statut, motif) => api.patch(`/stock/lots/${id}/status`, { statut, motif }),
};

export const inventoryService = {
  list: (params) => api.get(`/stock/inventories${toQuery(params)}`),
  get: (id) => api.get(`/stock/inventories/${id}`),
  create: (body) => api.post('/stock/inventories', body),
  count: (id, lignes) => api.put(`/stock/inventories/${id}/counts`, { lignes }),
  validate: (id) => api.post(`/stock/inventories/${id}/validate`),
  cancel: (id) => api.post(`/stock/inventories/${id}/cancel`),
};
