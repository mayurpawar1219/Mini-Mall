import { createContext, useContext } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cartApi } from '../api/cartApi';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

const CartContext = createContext(null);

export const CartProvider = ({ children }) => {
  const { isAuthenticated, role } = useAuth();
  const queryClient = useQueryClient();

  // Only fetch cart for customers
  const isCustomer = isAuthenticated && role === 'CUSTOMER';

  const { data: cartData, isLoading: isCartLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: cartApi.getCart,
    enabled: isCustomer,
  });

  const cartDataRaw = cartData?.data;
  
  // Transform backend format to frontend expected format
  const cart = cartDataRaw ? {
    ...cartDataRaw,
    items: cartDataRaw.items?.map(item => ({
      id: item.productId, // Frontend uses item.id for the product ID in update/remove calls
      quantity: item.quantity,
      subtotal: item.subtotal,
      product: {
        id: item.productId,
        name: item.productName,
        price: item.unitPrice,
        imageUrl: item.imageUrl,
        stockQuantity: item.stockQuantity
      }
    })) || []
  } : undefined;

  const addItemMutation = useMutation({
    mutationFn: ({ productId, quantity }) => cartApi.addItem(productId, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      toast.success('Added to cart');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to add item');
    },
  });

  const updateItemMutation = useMutation({
    mutationFn: ({ itemId, quantity }) => cartApi.updateItem(itemId, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });

  const removeItemMutation = useMutation({
    mutationFn: (itemId) => cartApi.removeItem(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });

  const clearCartMutation = useMutation({
    mutationFn: () => cartApi.clearCart(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });

  const addToCart = (productId, quantity = 1) => {
    addItemMutation.mutate({ productId, quantity });
  };

  const updateQuantity = (itemId, quantity) => {
    if (quantity <= 0) {
      removeItemMutation.mutate(itemId);
    } else {
      updateItemMutation.mutate({ itemId, quantity });
    }
  };

  const removeItem = (itemId) => {
    removeItemMutation.mutate(itemId);
  };

  const clearCart = () => {
    clearCartMutation.mutate();
  };

  const value = {
    cart,
    isLoading: isCartLoading || addItemMutation.isPending || updateItemMutation.isPending || removeItemMutation.isPending || clearCartMutation.isPending,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
  };

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};
