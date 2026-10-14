import { api } from './api';

export const settingsService = {
  get: () => api.get('/settings'),
  save: (body) => api.put('/settings', body),
};
