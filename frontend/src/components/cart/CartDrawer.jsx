import { X, Minus, Plus, ShoppingBag, ArrowRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useCartUI } from '../../context/CartUIContext';
import { Button } from '../ui/Button';

export default function CartDrawer() {
  const { isCartOpen, closeCart } = useCartUI();
  const { cart, updateQuantity, removeItem, isLoading } = useCart();
  const navigate = useNavigate();

  const handleCheckout = () => {
    closeCart();
    navigate('/checkout');
  };

  return (
    <AnimatePresence>
      {isCartOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeCart}
            className="fixed inset-0 bg-black/40 z-50 backdrop-blur-sm"
          />
          
          {/* Drawer */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed top-0 right-0 h-full w-full max-w-md bg-white z-50 shadow-2xl flex flex-col"
          >
            <div className="flex items-center justify-between p-4 border-b">
              <h2 className="text-lg font-semibold flex items-center">
                <ShoppingBag className="mr-2 h-5 w-5" /> Your Cart
              </h2>
              <Button variant="ghost" size="icon" onClick={closeCart}>
                <X className="h-5 w-5" />
              </Button>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4">
              {!cart || cart.items.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-center space-y-4">
                  <div className="bg-slate-100 p-4 rounded-full">
                    <ShoppingBag className="h-8 w-8 text-slate-400" />
                  </div>
                  <div className="space-y-1">
                    <h3 className="font-medium text-lg">Your cart is empty</h3>
                    <p className="text-sm text-muted-foreground">
                      Looks like you haven't added anything yet.
                    </p>
                  </div>
                  <Button onClick={closeCart} className="mt-4">
                    Continue Shopping
                  </Button>
                </div>
              ) : (
                <div className="space-y-4">
                  {cart.items.map((item) => (
                    <div key={item.id} className="flex gap-4 bg-slate-50 p-3 rounded-lg border">
                      <div className="w-20 h-20 bg-white rounded-md flex flex-shrink-0 items-center justify-center border overflow-hidden">
                        {item.product.imageUrl ? (
                          <img src={item.product.imageUrl} alt={item.product.name} className="w-full h-full object-cover" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }} />
                        ) : null}
                        <span className={`font-bold text-slate-400 text-xl ${item.product.imageUrl ? 'hidden' : ''}`}>{item.product.name.charAt(0)}</span>
                      </div>
                      
                      <div className="flex flex-col flex-1">
                        <div className="flex justify-between items-start">
                          <h4 className="font-medium text-sm line-clamp-2 pr-2">{item.product.name}</h4>
                          <button onClick={() => removeItem(item.id)} className="text-muted-foreground hover:text-destructive">
                            <X className="h-4 w-4" />
                          </button>
                        </div>
                        
                        <div className="mt-auto flex items-center justify-between">
                          <div className="font-bold">${item.subtotal.toFixed(2)}</div>
                          
                          <div className="flex items-center space-x-2 bg-white border rounded-md p-0.5">
                            <button 
                              onClick={() => updateQuantity(item.id, item.quantity - 1)}
                              disabled={isLoading}
                              className="w-6 h-6 flex items-center justify-center hover:bg-slate-100 rounded text-muted-foreground disabled:opacity-50"
                            >
                              <Minus className="h-3 w-3" />
                            </button>
                            <span className="text-sm font-medium w-4 text-center">{item.quantity}</span>
                            <button 
                              onClick={() => updateQuantity(item.id, item.quantity + 1)}
                              disabled={isLoading || item.quantity >= item.product.stockQuantity}
                              className="w-6 h-6 flex items-center justify-center hover:bg-slate-100 rounded text-muted-foreground disabled:opacity-50"
                            >
                              <Plus className="h-3 w-3" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            {cart && cart.items.length > 0 && (
              <div className="border-t p-4 bg-slate-50 space-y-4">
                <div className="space-y-1.5">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span className="font-medium">${cart.totalAmount.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Taxes & Fees</span>
                    <span className="font-medium">Calculated at checkout</span>
                  </div>
                </div>
                
                <div className="flex justify-between font-bold text-lg pt-2 border-t">
                  <span>Total</span>
                  <span>${cart.totalAmount.toFixed(2)}</span>
                </div>
                
                <Button className="w-full" size="lg" onClick={handleCheckout}>
                  Proceed to Checkout <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
