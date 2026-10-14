import { api, toQuery } from './api';

export const catalogService = {
  units: () => api.get('/catalog/units'),
  saveUnit: (id, body) => (id ? api.put(`/catalog/units/${id}`, body) : api.post('/catalog/units', body)),
  unitStatus: (id, actif) => api.patch(`/catalog/units/${id}/status`, { actif }),
  categories: () => api.get('/catalog/categories'),
  saveCategory: (id, body) => (id ? api.put(`/catalog/categories/${id}`, body) : api.post('/catalog/categories', body)),
  categoryStatus: (id, actif) => api.patch(`/catalog/categories/${id}/status`, { actif }),
  articles: (params) => api.get(`/catalog/articles${toQuery(params)}`),
  saveArticle: (id, body) => (id ? api.put(`/catalog/articles/${id}`, body) : api.post('/catalog/articles', body)),
  articleStatus: (id, actif) => api.patch(`/catalog/articles/${id}/status`, { actif }),
};
