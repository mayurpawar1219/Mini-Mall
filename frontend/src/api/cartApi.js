import apiClient from './client';

export const cartApi = {
  getCart: async () => {
    const response = await apiClient.get('/cart');
    return response.data;
  },
  
  addItem: async (productId, quantity) => {
    const response = await apiClient.post('/cart/items', { productId, quantity });
    return response.data;
  },
  
  updateItem: async (itemId, quantity) => {
    const response = await apiClient.put(`/cart/items/${itemId}`, { quantity });
    return response.data;
  },
  
  removeItem: async (itemId) => {
    const response = await apiClient.delete(`/cart/items/${itemId}`);
    return response.data;
  },
  
  clearCart: async () => {
    const response = await apiClient.delete('/cart');
    return response.data;
  }
};
