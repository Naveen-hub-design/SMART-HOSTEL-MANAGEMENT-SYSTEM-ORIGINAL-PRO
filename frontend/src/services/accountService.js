import API from './api';

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif'];

const apiOrigin = () => API.defaults.baseURL.replace(/\/api\/?$/, '');

const accountService = {
  getProfile: async () => {
    const response = await API.get('/account/profile');
    return response.data.data;
  },

  updateProfile: async (data) => {
    const response = await API.patch('/account/profile', data);
    return response.data.data;
  },

  changePassword: async (data) => {
    const response = await API.put('/account/password', data);
    return response.data;
  },

  uploadProfilePicture: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await API.put('/account/profile-picture', formData);
    return response.data;
  },

  isAllowedImage: (file) => {
    if (!file || !file.name || !file.name.includes('.')) return false;
    const ext = file.name.split('.').pop().toLowerCase();
    return IMAGE_EXTENSIONS.includes(ext);
  },

  resolveImageUrl: (path) => {
    if (!path) return '';
    if (/^https?:\/\//i.test(path)) return path;
    return `${apiOrigin()}${path.startsWith('/') ? path : `/${path}`}`;
  },
};

export default accountService;
