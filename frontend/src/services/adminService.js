import API from './api';

const adminService = {
  getDashboard: async () => {
    const response = await API.get('/admin/dashboard');
    return response.data.data;
  },

  createWarden: async (data) => {
    const response = await API.post('/admin/warden', data);
    return response.data;
  },

  deleteWarden: async (id) => {
    const response = await API.delete(`/admin/warden/${id}`);
    return response.data;
  },

  createHostelBlock: async (data) => {
    const response = await API.post('/admin/hostel-block', data);
    return response.data;
  },

  getHostelBlocks: async () => {
    const response = await API.get('/admin/hostel-blocks');
    return response.data.data;
  },

  getStudents: async () => {
    const response = await API.get('/admin/students');
    return response.data.data;
  },

  deleteStudent: async (id) => {
    const response = await API.delete(`/admin/students/${id}`);
    return response.data;
  },

  getReports: async () => {
    const response = await API.get('/admin/reports');
    return response.data.data;
  },

  getAllWardens: async () => {
    const response = await API.get('/wardens');
    return response.data.data;
  },

  getAuditLogs: async (params = {}) => {
    const response = await API.get('/v1/admin/audit-logs', { params });
    return response.data;
  },
};

export default adminService;
