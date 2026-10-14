import { api } from './api';

export const userService = {
  list: () => api.get('/users'),
  roles: () => api.get('/roles'),
  permissions: () => api.get('/permissions'),
  create: (payload) => api.post('/users', payload),
  update: (id, payload) => api.put(`/users/${id}`, payload),
  resetPassword: (id, temporaryPassword) => api.post(`/users/${id}/reset-password`, { temporaryPassword }),
  remove: (id) => api.delete(`/users/${id}`),
  setActive: (id, active) => api.patch(`/users/${id}/status`, { active }),
  saveRole: (id, body) => (id ? api.put(`/roles/${id}`, body) : api.post('/roles', body)),
  deleteRole: (id) => api.delete(`/roles/${id}`),
};
