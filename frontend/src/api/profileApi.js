import apiClient from './client';

export const profileApi = {
  getProfile: async () => {
    const response = await apiClient.get('/profile');
    return response.data;
  },

  updateProfile: async (data) => {
    const response = await apiClient.put('/profile', data);
    return response.data;
  },

  changePassword: async (data) => {
    const response = await apiClient.put('/profile/password', data);
    return response.data;
  },
};
