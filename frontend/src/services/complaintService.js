import API from './api';

const complaintService = {
  createComplaint: async (formData) => {
    const response = await API.post('/complaints', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  getMyComplaints: async () => {
    const response = await API.get('/complaints/my');
    return response.data.data;
  },

  getAllComplaints: async () => {
    const response = await API.get('/complaints');
    return response.data.data;
  },

  updateStatus: async (id, status) => {
    const response = await API.put(`/complaints/${id}/status`, { status });
    return response.data;
  },

  getComplaintStats: async () => {
    const response = await API.get('/complaints/stats');
    return response.data.data;
  },
};

export default complaintService;
