import apiClient from './client';

export const productApi = {
  getAll: async (params = {}) => {
    // The backend returns List, not Page. Handle search and category via dedicated endpoints.
    if (params.search) {
      const response = await apiClient.get('/products/search', { params: { keyword: params.search } });
      return response.data;
    }
    if (params.categoryId) {
      const response = await apiClient.get(`/products/category/${params.categoryId}`);
      return response.data;
    }
    const response = await apiClient.get('/products', { params: { activeOnly: true } });
    return response.data;
  },

  getById: async (id) => {
    const response = await apiClient.get(`/products/${id}`);
    return response.data;
  },

  getCategories: async () => {
    const response = await apiClient.get('/categories');
    return response.data;
  },

  createCategory: async (data) => {
    const response = await apiClient.post('/categories', data);
    return response.data;
  },

  updateCategory: async (id, data) => {
    const response = await apiClient.put(`/categories/${id}`, data);
    return response.data;
  },

  deleteCategory: async (id) => {
    const response = await apiClient.delete(`/categories/${id}`);
    return response.data;
  },

  create: async (data) => {
    const response = await apiClient.post('/products', data);
    return response.data;
  },

  update: async (id, data) => {
    const response = await apiClient.put(`/products/${id}`, data);
    return response.data;
  },

  updateStatus: async (id, active) => {
    const response = await apiClient.patch(`/products/${id}/status`, { active });
    return response.data;
  }
};
