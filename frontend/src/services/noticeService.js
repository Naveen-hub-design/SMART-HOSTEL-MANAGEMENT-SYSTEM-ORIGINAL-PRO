import API from './api';

const noticeService = {
  getAllNotices: async () => {
    const response = await API.get('/notices');
    return response.data.data;
  },

  createNotice: async (data) => {
    const response = await API.post('/notices', data);
    return response.data;
  },

  updateNotice: async (id, data) => {
    const response = await API.put(`/notices/${id}`, data);
    return response.data;
  },

  deleteNotice: async (id) => {
    const response = await API.delete(`/notices/${id}`);
    return response.data;
  },
};

export default noticeService;
