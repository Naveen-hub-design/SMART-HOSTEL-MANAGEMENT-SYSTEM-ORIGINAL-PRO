import API from './api';

const lostFoundService = {
  getAllItems: async () => {
    const response = await API.get('/lost-found');
    return response.data.data;
  },

  getMyItems: async () => {
    const response = await API.get('/lost-found/my');
    return response.data.data;
  },

  reportItem: async (formData) => {
    const response = await API.post('/lost-found', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  updateStatus: async (id, status) => {
    const response = await API.put(`/lost-found/${id}/status`, { status });
    return response.data;
  },
};

export default lostFoundService;
