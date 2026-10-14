import { api, toQuery } from './api';

export const auditService = {
  search: (params) => api.get(`/audit${toQuery(params)}`),
};
