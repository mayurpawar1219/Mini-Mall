import apiClient from './client';

export const checkoutApi = {
  checkout: async (data) => {
    const response = await apiClient.post('/checkout', data);
    return response.data;
  },

  createPaymentIntent: async () => {
    const response = await apiClient.post('/checkout/payment-intent');
    return response.data;
  },
  
  getPickupSlots: async (date) => {
    // Assuming backend endpoint is /pickup-slots?date=YYYY-MM-DD
    const response = await apiClient.get('/pickup-slots', { params: { date } });
    return response.data;
  }
};
