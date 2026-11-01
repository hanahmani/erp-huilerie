import { api, toQuery } from './api';

export const salesDocService = {
  list: (params) => api.get(`/sales-documents${toQuery(params)}`),
  get: (id) => api.get(`/sales-documents/${id}`),
  save: (id, body) => (id ? api.put(`/sales-documents/${id}`, body) : api.post('/sales-documents', body)),
  validate: (id) => api.post(`/sales-documents/${id}/validate`),
  transform: (id, cible) => api.post(`/sales-documents/${id}/transform`, { cible }),
  pay: (id, body) => api.post(`/sales-documents/${id}/pay`, body),
  creditNote: (id, body) => api.post(`/sales-documents/${id}/credit-note`, body),
  cancel: (id, motif) => api.post(`/sales-documents/${id}/cancel`, { motif }),
};

export const hrService = {
  employees: (params) => api.get(`/hr/employees${toQuery(params)}`),
  employee: (id) => api.get(`/hr/employees/${id}`),
  saveEmployee: (id, body) => (id ? api.put(`/hr/employees/${id}`, body) : api.post('/hr/employees', body)),
  setStatus: (id, actif) => api.patch(`/hr/employees/${id}/status`, { actif }),
  start: (id) => api.post(`/hr/employees/${id}/start`),
  stop: (id) => api.post(`/hr/employees/${id}/stop`),
  workDays: (params) => api.get(`/hr/work-days${toQuery(params)}`),
  addWorkDay: (body) => api.post('/hr/work-days', body),
  updateWorkDay: (id, body) => api.put(`/hr/work-days/${id}`, body),
  deleteWorkDay: (id) => api.delete(`/hr/work-days/${id}`),
  pay: (id, body) => api.post(`/hr/employees/${id}/payments`, body),
  payments: (params) => api.get(`/hr/payments${toQuery(params)}`),
};

export const notificationService = {
  list: () => api.get('/notifications'),
  read: (keys) => api.post('/notifications/read', { keys }),
};

export const searchService = {
  search: (q) => api.get(`/search${toQuery({ q })}`),
};
