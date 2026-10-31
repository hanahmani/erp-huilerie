import { api } from './api';

export const authService = {
  login: (username, password, rememberMe) => api.post('/auth/login', { username, password, rememberMe }),
  me: () => api.get('/auth/me'),
  changePassword: (currentPassword, newPassword) =>
    api.post('/auth/change-password', { currentPassword, newPassword }),
};
