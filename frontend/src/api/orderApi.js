import apiClient from './client';

export const orderApi = {
  getMyOrders: async (page = 0, size = 20) => {
    const response = await apiClient.get('/orders', { params: { page, size } });
    return response.data;
  },

  getOrderById: async (id) => {
    const response = await apiClient.get(`/orders/${id}`);
    return response.data;
  },

  cancelOrder: async (id) => {
    const response = await apiClient.post(`/orders/${id}/cancel`);
    return response.data;
  }
};

export const returnExchangeApi = {
  requestReturn: async (orderId, data) => {
    const response = await apiClient.post(`/returns/customer/request/${orderId}`, data);
    return response.data;
  },

  requestExchange: async (orderId, data) => {
    const response = await apiClient.post(`/exchanges/customer/request/${orderId}`, data);
    return response.data;
  }
};
