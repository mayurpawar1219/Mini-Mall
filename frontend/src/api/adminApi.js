import apiClient from './client';

export const adminApi = {
  getDashboardStats: async () => {
    const response = await apiClient.get('/dashboard/admin/stats');
    return response.data;
  },
  
  getSalesTrends: async (params) => {
    const response = await apiClient.get('/dashboard/admin/sales-trends', { params });
    return response.data;
  },
  
  getLowStockProducts: async () => {
    const response = await apiClient.get('/dashboard/admin/low-stock');
    return response.data;
  },

  // Pickup Slots
  getPickupSlots: async (date) => {
    const params = date ? { date } : {};
    const response = await apiClient.get('/admin/pickup-slots', { params });
    return response.data;
  },
  
  createPickupSlot: async (data) => {
    const response = await apiClient.post('/admin/pickup-slots', data);
    return response.data;
  },
  
  updatePickupSlot: async (id, data) => {
    const response = await apiClient.patch(`/admin/pickup-slots/${id}`, data);
    return response.data;
  },
  
  deletePickupSlot: async (id) => {
    const response = await apiClient.delete(`/admin/pickup-slots/${id}`);
    return response.data;
  },

  // Users
  getUsers: async (params) => {
    const response = await apiClient.get('/admin/users', { params });
    return response.data;
  },

  // Orders
  getOrders: async (params) => {
    const response = await apiClient.get('/admin/orders', { params });
    return response.data;
  },
  getOrder: async (id) => {
    const response = await apiClient.get(`/admin/orders/${id}`);
    return response.data;
  },
  updateOrderStatus: async (id, status) => {
    const response = await apiClient.patch(`/admin/orders/${id}/status`, { status });
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

  // Audit Logs
  getAuditLogs: async (params) => {
    const response = await apiClient.get('/admin/audit-logs', { params });
    return response.data;
  }
};
