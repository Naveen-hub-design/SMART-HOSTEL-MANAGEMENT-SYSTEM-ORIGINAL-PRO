import API from './api';

const roomService = {
  getAllRooms: async () => {
    const response = await API.get('/rooms');
    return response.data.data;
  },

  getRoomById: async (id) => {
    const response = await API.get(`/rooms/${id}`);
    return response.data.data;
  },

  getAvailableRooms: async () => {
    const response = await API.get('/rooms/available');
    return response.data.data;
  },

  getRoomsByBlock: async (blockId) => {
    const response = await API.get(`/rooms/block/${blockId}`);
    return response.data.data;
  },

  addRoom: async (data) => {
    const response = await API.post('/rooms', data);
    return response.data;
  },

  updateRoom: async (id, data) => {
    const response = await API.put(`/rooms/${id}`, data);
    return response.data;
  },

  deleteRoom: async (id) => {
    const response = await API.delete(`/rooms/${id}`);
    return response.data;
  },

  allocateRoom: async (roomId, studentId) => {
    const response = await API.post(`/rooms/${roomId}/allocate/${studentId}`);
    return response.data;
  },

  vacateRoom: async (studentId) => {
    const response = await API.post(`/rooms/vacate/${studentId}`);
    return response.data;
  },
};

export default roomService;
