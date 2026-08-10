import API from './api';

const studentService = {
  getProfile: async () => {
    const response = await API.get('/student/profile');
    return response.data.data;
  },

  updateProfile: async (data) => {
    const response = await API.put('/student/profile', data);
    return response.data.data;
  },

  getMyRoom: async () => {
    const response = await API.get('/student/my-room');
    return response.data.data;
  },

  changePassword: async (data) => {
    const response = await API.put('/student/change-password', data);
    return response.data;
  },
};

export default studentService;
