import API from './api';

const messFeedbackService = {
  submitFeedback: async (data) => {
    const response = await API.post('/mess-feedback', data);
    return response.data;
  },

  getMyFeedback: async () => {
    const response = await API.get('/mess-feedback/my');
    return response.data.data;
  },

  getAllFeedback: async () => {
    const response = await API.get('/mess-feedback/all');
    return response.data.data;
  },

  getAverages: async () => {
    const response = await API.get('/mess-feedback/averages');
    return response.data.data;
  },
};

export default messFeedbackService;
