import API from './api';

const authService = {
  login: async (email, password) => {
    const response = await API.post('/auth/login', { email, password });
    const authData = response.data.data;
    if (authData && authData.token) {
      localStorage.setItem('token', authData.token);
      const userData = {
        id: authData.userId,
        name: authData.name,
        email: authData.email,
        role: (authData.role || '').toLowerCase().replace(/^role_/, ''),
      };
      localStorage.setItem('user', JSON.stringify(userData));
    }
    return authData;
  },

  register: async (data) => {
    const response = await API.post('/auth/register', data);
    const authData = response.data.data;
    if (authData && authData.token) {
      localStorage.setItem('token', authData.token);
      const userData = {
        id: authData.userId,
        name: authData.name,
        email: authData.email,
        role: (authData.role || '').toLowerCase().replace(/^role_/, ''),
      };
      localStorage.setItem('user', JSON.stringify(userData));
    }
    return authData;
  },

  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  },
};

export default authService;
