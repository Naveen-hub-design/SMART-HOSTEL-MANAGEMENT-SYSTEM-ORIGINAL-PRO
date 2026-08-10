import API from './api';

const aiService = {
  categorizeComplaint: async (title, description) => {
    const response = await API.post('/ai/categorize-complaint', {
      complaintTitle: title,
      complaintDescription: description,
    });
    return response.data;
  },

  analyzeSentiment: async (feedbackText) => {
    const response = await API.post('/ai/analyze-sentiment', { feedbackText });
    return response.data;
  },

  getRecommendedRooms: async () => {
    const response = await API.get('/ai/recommend-room');
    return response.data.data;
  },
};

export default aiService;
