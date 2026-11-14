import { api, API_BASE_URL } from './api';

export const settingsService = {
  get: () => api.get('/settings'),
  save: (body) => api.put('/settings', body),
  uploadLogo: (file) => api.upload('PUT', '/settings/logo', 'fichier', file),
  deleteLogo: () => api.delete('/settings/logo'),
  /** URL publique du logo téléversé ; v force le rechargement après un changement. */
  logoUrl: (v) => `${API_BASE_URL}/settings/logo${v ? `?v=${v}` : ''}`,
};
