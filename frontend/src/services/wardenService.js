import API from './api';

const wardenService = {
  getDashboard: async () => {
    const response = await API.get('/wardens/dashboard');
    return response.data.data;
  },

  getStudents: async ({ search, gender, roomStatus, page, size } = {}) => {
    const response = await API.get('/wardens/students', {
      params: { search, gender, roomStatus, page, size },
    });
    return response.data.data;
  },

  getStudentDetails: async (id) => {
    const response = await API.get(`/wardens/students/${id}`);
    return response.data.data;
  },

  assignBlock: async (wardenId, blockId) => {
    const response = await API.post(`/wardens/assign-block/${wardenId}/${blockId}`);
    return response.data;
  },

  createStudent: async (studentData) => {
    const response = await API.post('/wardens/student', studentData);
    return response.data.data;
  },

  bulkImportStudents: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await API.post('/wardens/students/bulk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data.data;
  },
};

export default wardenService;
