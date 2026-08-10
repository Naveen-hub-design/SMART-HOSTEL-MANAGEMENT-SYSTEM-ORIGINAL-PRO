import API from './api';

const wardenService = {
  getDashboard: async () => {
    const response = await API.get('/wardens/dashboard');
    return response.data.data;
  },

  getStudents: async () => {
    const response = await API.get('/wardens/students');
    return response.data.data;
  },

  assignBlock: async (wardenId, blockId) => {
    const response = await API.post(`/wardens/assign-block/${wardenId}/${blockId}`);
    return response.data;
  },
};

export default wardenService;
