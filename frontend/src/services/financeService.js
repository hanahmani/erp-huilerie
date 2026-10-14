import { api, toQuery } from './api';

export const financeService = {
  cash: (params) => api.get(`/finance/cash${toQuery(params)}`),
  expenses: (params) => api.get(`/finance/expenses${toQuery(params)}`),
  createExpense: (body) => api.post('/finance/expenses', body),
  cancelExpense: (id, motif) => api.post(`/finance/expenses/${id}/cancel`, { motif }),
  session: () => api.get('/finance/session'),
  sessions: (params) => api.get(`/finance/sessions${toQuery(params)}`),
  openSession: (fondInitial) => api.post('/finance/session/open', { fondInitial }),
  closeSession: (especesComptees, justification) => api.post('/finance/session/close', { especesComptees, justification }),
};
