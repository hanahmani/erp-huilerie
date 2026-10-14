import { api } from './api';

export const referenceService = {
  taxes: () => api.get('/settings/taxes'),
  saveTax: (id, body) => (id ? api.put(`/settings/taxes/${id}`, body) : api.post('/settings/taxes', body)),
  taxStatus: (id, actif) => api.patch(`/settings/taxes/${id}/status`, { actif }),
  expenseCategories: () => api.get('/settings/expense-categories'),
  saveExpenseCategory: (id, body) => (id ? api.put(`/settings/expense-categories/${id}`, body) : api.post('/settings/expense-categories', body)),
  expenseCategoryStatus: (id, actif) => api.patch(`/settings/expense-categories/${id}/status`, { actif }),
};
