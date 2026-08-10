import API from './api';

const marketplaceService = {
  getAllItems: async () => {
    const response = await API.get('/marketplace');
    return response.data.data;
  },

  getMyItems: async () => {
    const response = await API.get('/marketplace/my');
    return response.data.data;
  },

  addItem: async (formData) => {
    const response = await API.post('/marketplace', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  markAsSold: async (id) => {
    const response = await API.put(`/marketplace/${id}/sold`);
    return response.data;
  },

  deleteItem: async (id) => {
    const response = await API.delete(`/marketplace/${id}`);
    return response.data;
  },

  getByCategory: async (category) => {
    const response = await API.get(`/marketplace/category/${category}`);
    return response.data.data;
  },
};

export default marketplaceService;
