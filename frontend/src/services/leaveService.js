import API from './api';

const leaveService = {
  applyLeave: async (data) => {
    const response = await API.post('/leaves', data);
    return response.data;
  },

  getMyLeaves: async () => {
    const response = await API.get('/leaves/my');
    return response.data.data;
  },

  getAllLeaves: async () => {
    const response = await API.get('/leaves');
    return response.data.data;
  },

  getPendingLeaves: async () => {
    const response = await API.get('/leaves/pending');
    return response.data.data;
  },

  approveLeave: async (id) => {
    const response = await API.put(`/leaves/${id}/approve`);
    return response.data;
  },

  rejectLeave: async (id, data) => {
    const response = await API.put(`/leaves/${id}/reject`, data);
    return response.data;
  },

  getLeaveStats: async () => {
    const response = await API.get('/leaves/stats');
    return response.data.data;
  },
};

export default leaveService;
