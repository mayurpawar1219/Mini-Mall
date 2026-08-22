import apiClient from './client';

export const staffApi = {
  getDashboardStats: async () => {
    const response = await apiClient.get('/dashboard/staff');
    return response.data;
  },
  
  // Orders
  getOrders: async (params) => {
    const response = await apiClient.get('/admin/orders', { params });
    return response.data;
  },
  updateOrderStatus: async (orderId, status) => {
    const response = await apiClient.patch(`/admin/orders/${orderId}/status`, { status });
    return response.data;
  },

  // Returns
  getReturns: async (params) => {
    const response = await apiClient.get('/admin/returns', { params });
    return response.data;
  },
  updateReturnStatus: async (id, status) => {
    const response = await apiClient.patch(`/admin/returns/${id}/status`, { status });
    return response.data;
  },

  // Exchanges
  getExchanges: async (params) => {
    const response = await apiClient.get('/admin/exchanges', { params });
    return response.data;
  },
  updateExchangeStatus: async (id, status) => {
    const response = await apiClient.patch(`/admin/exchanges/${id}/status`, { status });
    return response.data;
  },

  // Inventory (Read-Only)
  getLowStockProducts: async () => {
    const response = await apiClient.get('/dashboard/admin/low-stock');
    return response.data;
  },
};
