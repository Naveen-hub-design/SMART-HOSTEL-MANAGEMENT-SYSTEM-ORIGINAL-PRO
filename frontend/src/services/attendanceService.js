import API from './api';

const attendanceService = {
  getAttendance: async ({ date, status, page, size } = {}) => {
    const response = await API.get('/attendance', {
      params: { date, status, page, size },
    });
    return response.data.data;
  },

  getMyAttendance: async ({ date, status, page, size } = {}) => {
    const response = await API.get('/attendance/my', {
      params: { date, status, page, size },
    });
    return response.data.data;
  },

  markAttendance: async (data) => {
    const response = await API.post('/attendance/mark', data);
    return response.data;
  },

  markBulkAttendance: async (data) => {
    const response = await API.post('/attendance/mark-bulk', data);
    return response.data;
  },

  updateAttendance: async (id, data) => {
    const response = await API.put(`/attendance/${id}`, data);
    return response.data;
  },
};

export default attendanceService;
