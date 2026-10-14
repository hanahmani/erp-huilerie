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

export const qualityService = {
  criteria: () => api.get('/quality/criteria'),
  saveCriterion: (id, body) => (id ? api.put(`/quality/criteria/${id}`, body) : api.post('/quality/criteria', body)),
  criterionStatus: (id, actif) => api.patch(`/quality/criteria/${id}/status`, { actif }),
  controls: (params) => api.get(`/quality/controls${toQuery(params)}`),
  record: (body) => api.post('/quality/controls', body),
  anomalies: (params) => api.get(`/quality/anomalies${toQuery(params)}`),
  createAnomaly: (body) => api.post('/quality/anomalies', body),
  updateAnomaly: (id, body) => api.put(`/quality/anomalies/${id}`, body),
};

export const hrService = {
  services: () => api.get('/hr/services'),
  saveService: (id, nom) => (id ? api.put(`/hr/services/${id}`, { nom }) : api.post('/hr/services', { nom })),
  positions: () => api.get('/hr/positions'),
  savePosition: (id, nom) => (id ? api.put(`/hr/positions/${id}`, { nom }) : api.post('/hr/positions', { nom })),
  employees: (params) => api.get(`/hr/employees${toQuery(params)}`),
  saveEmployee: (id, body) => (id ? api.put(`/hr/employees/${id}`, body) : api.post('/hr/employees', body)),
  me: () => api.get('/hr/me'),
  leaves: (params) => api.get(`/hr/leaves${toQuery(params)}`),
  requestLeave: (body) => api.post('/hr/leaves', body),
  decide: (id, accepter, motifRefus) => api.post(`/hr/leaves/${id}/decision`, { accepter, motifRefus }),
  cancelLeave: (id) => api.post(`/hr/leaves/${id}/cancel`),
  workingDays: (from, to) => api.get(`/hr/leaves/working-days${toQuery({ from, to })}`),
  absences: (params) => api.get(`/hr/absences${toQuery(params)}`),
  recordAbsence: (body) => api.post('/hr/absences', body),
  holidays: () => api.get('/hr/holidays'),
  addHoliday: (body) => api.post('/hr/holidays', body),
  deleteHoliday: (id) => api.delete(`/hr/holidays/${id}`),
};

export const notificationService = {
  list: () => api.get('/notifications'),
  read: (keys) => api.post('/notifications/read', { keys }),
};

export const searchService = {
  search: (q) => api.get(`/search${toQuery({ q })}`),
};
