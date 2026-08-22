import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { checkoutApi } from '../../api/checkoutApi';
import { useCart } from '../../context/CartContext';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { MapPin, Truck, CalendarClock, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { motion } from 'framer-motion';

import { loadStripe } from '@stripe/stripe-js';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';

// Initialize Stripe outside component render
const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || 'pk_test_placeholder');

const deliverySchema = z.object({
  addressLine1: z.string().min(5, 'Address is required'),
  addressLine2: z.string().optional(),
  city: z.string().min(2, 'City is required'),
  postalCode: z.string().min(4, 'Postal code is required'),
  phone: z.string().min(10, 'Phone is required').optional(), // Added phone
});

function PaymentForm({ clientSecret, orderPayload, onSuccess, onCancel }) {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const checkoutMutation = useMutation({
    mutationFn: (data) => checkoutApi.checkout(data),
    onSuccess: (res) => {
      onSuccess(res.data);
    },
    onError: (err) => {
      console.error(err);
      setIsProcessing(false);
      setErrorMessage(err.response?.data?.message || 'Checkout failed.');
    }
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setIsProcessing(true);
    setErrorMessage(null);

    // 1. Confirm Stripe Payment
    const { error, paymentIntent } = await stripe.confirmPayment({
      elements,
      redirect: 'if_required',
    });

    if (error) {
      setErrorMessage(error.message);
      setIsProcessing(false);
    } else if (paymentIntent && paymentIntent.status === 'succeeded') {
      // 2. Complete order in backend
      const payload = {
        ...orderPayload,
        paymentIntentId: paymentIntent.id,
      };
      checkoutMutation.mutate(payload);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <PaymentElement />
      
      {errorMessage && (
        <div className="text-red-500 text-sm font-medium p-3 bg-red-50 rounded-md">
          {errorMessage}
        </div>
      )}

      <div className="flex gap-4 justify-end mt-6">
        <Button variant="outline" type="button" onClick={onCancel} disabled={isProcessing}>
          Back
        </Button>
        <Button type="submit" disabled={!stripe || isProcessing} isLoading={isProcessing}>
          Pay Now
        </Button>
      </div>
    </form>
  );
}

export default function Checkout() {
  const [step, setStep] = useState(1);
  const [orderType, setOrderType] = useState('STORE_PICKUP');
  const [selectedSlotId, setSelectedSlotId] = useState(null);
  const [pickupDate, setPickupDate] = useState(new Date().toISOString().split('T')[0]);
  const [completedOrder, setCompletedOrder] = useState(null);
  
  const [clientSecret, setClientSecret] = useState('');
  const [orderPayload, setOrderPayload] = useState(null);

  const { cart, clearCart } = useCart();
  const navigate = useNavigate();

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(deliverySchema),
  });

  const { data: slotsData, isLoading: slotsLoading } = useQuery({
    queryKey: ['pickup-slots', pickupDate],
    queryFn: () => checkoutApi.getPickupSlots(pickupDate),
    enabled: orderType === 'SCHEDULED_PICKUP',
  });

  const intentMutation = useMutation({
    mutationFn: () => checkoutApi.createPaymentIntent(),
    onSuccess: (res) => {
      setClientSecret(res.data.clientSecret);
      setStep(3);
    },
    onError: (err) => {
      console.error(err);
      alert('Failed to initialize payment: ' + (err.response?.data?.message || err.message));
    }
  });

  if (!cart || cart.items.length === 0) {
    if (step === 4) {
      // Show success
    } else {
      return (
        <div className="container py-20 text-center max-w-md mx-auto">
          <div className="bg-slate-100 p-6 rounded-full inline-block mb-6">
            <Truck className="h-12 w-12 text-slate-400" />
          </div>
          <h2 className="text-2xl font-bold mb-2">Your cart is empty</h2>
          <p className="text-muted-foreground mb-8">You need items in your cart to checkout.</p>
          <Button onClick={() => navigate('/products')} className="w-full">Return to Shop</Button>
        </div>
      );
    }
  }

  const handleNextStep = () => {
    if (step === 1 && orderType === 'HOME_DELIVERY') {
      setStep(2);
    } else if (step === 1 && orderType === 'SCHEDULED_PICKUP') {
      setStep(2);
    } else if (step === 1 && orderType === 'STORE_PICKUP') {
      preparePayment({}); // empty details
    }
  };

  const preparePayment = (deliveryData = {}) => {
    const payload = {
      type: orderType,
    };
    
    if (orderType === 'HOME_DELIVERY') {
      payload.deliveryAddress = deliveryData.addressLine1 + (deliveryData.addressLine2 ? ', ' + deliveryData.addressLine2 : '');
      payload.deliveryCity = deliveryData.city;
      payload.deliveryPostalCode = deliveryData.postalCode;
      payload.deliveryPhone = deliveryData.phone || '0000000000';
    } else if (orderType === 'SCHEDULED_PICKUP') {
      payload.pickupSlotId = selectedSlotId;
    }
    
    setOrderPayload(payload);
    intentMutation.mutate();
  };

  if (step === 4 && completedOrder) {
    return (
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="container py-16 px-4 flex flex-col items-center max-w-lg mx-auto text-center"
      >
        <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mb-6">
          <CheckCircle2 className="w-10 h-10" />
        </div>
        <h1 className="text-3xl font-bold mb-2">Order Confirmed!</h1>
        <p className="text-muted-foreground mb-8">
          Thank you for your purchase. Your order number is <span className="font-bold text-foreground">{completedOrder.orderNumber}</span>.
        </p>
        <div className="bg-slate-50 border rounded-xl p-6 w-full text-left mb-8">
          <div className="flex justify-between mb-2">
            <span className="text-muted-foreground">Total</span>
            <span className="font-bold">${completedOrder.totalAmount?.toFixed(2) || '0.00'}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Type</span>
            <span className="font-medium">
              {completedOrder.type?.replace('_', ' ') || 'Order'}
            </span>
          </div>
        </div>
        <Button onClick={() => navigate('/dashboard')} className="w-full h-12 text-lg">
          View Order Status
        </Button>
      </motion.div>
    );
  }

  return (
    <div className="bg-slate-50 min-h-[calc(100vh-64px)] py-8">
      <div className="container px-4 md:px-6">
        <button onClick={() => navigate(-1)} className="flex items-center text-sm font-medium text-muted-foreground hover:text-primary mb-6 transition-colors">
          <ArrowLeft className="mr-2 h-4 w-4" /> Back
        </button>
        
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            
            <Card className={step === 1 ? 'border-primary shadow-md' : 'opacity-70'}>
              <CardHeader className="flex flex-row items-center justify-between bg-slate-50/50 border-b pb-4">
                <CardTitle className="flex items-center gap-2">
                  <span className="bg-primary text-primary-foreground w-6 h-6 rounded-full flex items-center justify-center text-sm">1</span> 
                  Fulfillment Method
                </CardTitle>
                {step > 1 && (
                  <Button variant="link" onClick={() => setStep(1)}>Edit</Button>
                )}
              </CardHeader>
              {step === 1 && (
                <CardContent className="p-6">
                  <div className="grid sm:grid-cols-3 gap-4">
                    <button 
                      onClick={() => setOrderType('STORE_PICKUP')}
                      className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all ${orderType === 'STORE_PICKUP' ? 'border-primary bg-primary/5 text-primary' : 'border-slate-200 hover:border-slate-300'}`}
                    >
                      <MapPin className="h-8 w-8 mb-3" />
                      <span className="font-semibold">Store Pickup</span>
                      <span className="text-xs mt-1 text-center opacity-80">Pick up anytime today</span>
                    </button>
                    <button 
                      onClick={() => setOrderType('SCHEDULED_PICKUP')}
                      className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all ${orderType === 'SCHEDULED_PICKUP' ? 'border-primary bg-primary/5 text-primary' : 'border-slate-200 hover:border-slate-300'}`}
                    >
                      <CalendarClock className="h-8 w-8 mb-3" />
                      <span className="font-semibold">Scheduled Pickup</span>
                      <span className="text-xs mt-1 text-center opacity-80">Reserve a time slot</span>
                    </button>
                    <button 
                      onClick={() => setOrderType('HOME_DELIVERY')}
                      className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all ${orderType === 'HOME_DELIVERY' ? 'border-primary bg-primary/5 text-primary' : 'border-slate-200 hover:border-slate-300'}`}
                    >
                      <Truck className="h-8 w-8 mb-3" />
                      <span className="font-semibold">Home Delivery</span>
                      <span className="text-xs mt-1 text-center opacity-80">Delivered to your door</span>
                    </button>
                  </div>
                  <div className="mt-8 flex justify-end">
                    <Button onClick={handleNextStep}>Continue</Button>
                  </div>
                </CardContent>
              )}
            </Card>

            {(orderType === 'HOME_DELIVERY' || orderType === 'SCHEDULED_PICKUP') && (
              <Card className={step === 2 ? 'border-primary shadow-md' : 'opacity-70'}>
                <CardHeader className="flex flex-row items-center justify-between bg-slate-50/50 border-b pb-4">
                  <CardTitle className="flex items-center gap-2">
                    <span className="bg-primary text-primary-foreground w-6 h-6 rounded-full flex items-center justify-center text-sm">2</span> 
                    {orderType === 'HOME_DELIVERY' ? 'Delivery Address' : 'Select Time Slot'}
                  </CardTitle>
                  {step > 2 && (
                    <Button variant="link" onClick={() => setStep(2)}>Edit</Button>
                  )}
                </CardHeader>
                {step === 2 && (
                  <CardContent className="p-6">
                    {orderType === 'HOME_DELIVERY' ? (
                      <form id="delivery-form" onSubmit={handleSubmit((data) => {
                        preparePayment(data);
                      })} className="space-y-4">
                        <div className="space-y-2">
                          <label className="text-sm font-medium">Address Line 1</label>
                          <Input error={errors.addressLine1?.message} {...register('addressLine1')} placeholder="123 Main St" />
                        </div>
                        <div className="space-y-2">
                          <label className="text-sm font-medium">Address Line 2 (Optional)</label>
                          <Input error={errors.addressLine2?.message} {...register('addressLine2')} placeholder="Apt 4B" />
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <label className="text-sm font-medium">City</label>
                            <Input error={errors.city?.message} {...register('city')} />
                          </div>
                          <div className="space-y-2">
                            <label className="text-sm font-medium">Postal Code</label>
                            <Input error={errors.postalCode?.message} {...register('postalCode')} />
                          </div>
                        </div>
                        <div className="space-y-2">
                          <label className="text-sm font-medium">Phone</label>
                          <Input error={errors.phone?.message} {...register('phone')} placeholder="Phone Number" />
                        </div>
                        <div className="mt-6 flex justify-end">
                          <Button type="submit" isLoading={intentMutation.isPending}>Continue to Payment</Button>
                        </div>
                      </form>
                    ) : (
                      <div className="space-y-4">
                        <div className="flex gap-4 items-center mb-6">
                          <label className="text-sm font-medium">Date:</label>
                          <Input 
                            type="date" 
                            className="w-auto" 
                            value={pickupDate}
                            min={new Date().toISOString().split('T')[0]}
                            onChange={(e) => setPickupDate(e.target.value)}
                          />
                        </div>
                        
                        {slotsLoading ? (
                          <div className="text-center py-8">Loading slots...</div>
                        ) : slotsData?.data?.length === 0 ? (
                          <div className="text-center py-8 bg-amber-50 rounded-lg text-amber-800 border border-amber-200">
                            No slots available for this date.
                          </div>
                        ) : (
                          <div className="grid sm:grid-cols-2 gap-4">
                            {slotsData?.data?.map(slot => {
                              const isFull = !slot.available || slot.currentBookings >= slot.capacity;
                              return (
                                <button
                                  key={slot.id}
                                  disabled={isFull}
                                  onClick={() => setSelectedSlotId(slot.id)}
                                  className={`p-4 rounded-xl border text-left flex justify-between items-center transition-all ${
                                    selectedSlotId === slot.id 
                                      ? 'border-primary ring-2 ring-primary/20 bg-primary/5' 
                                      : isFull 
                                        ? 'opacity-50 cursor-not-allowed bg-slate-50' 
                                        : 'hover:border-slate-300'
                                  }`}
                                >
                                  <div>
                                    <div className="font-medium text-lg">
                                      {slot.startTime.split('T')[1].substring(0,5)} - {slot.endTime.split('T')[1].substring(0,5)}
                                    </div>
                                    <div className="text-sm text-muted-foreground mt-1">
                                      {isFull ? 'Fully Booked' : `${slot.capacity - slot.currentBookings} spots left`}
                                    </div>
                                  </div>
                                  {selectedSlotId === slot.id && <CheckCircle2 className="text-primary h-6 w-6" />}
                                </button>
                              );
                            })}
                          </div>
                        )}
                        
                        <div className="mt-8 flex justify-end">
                          <Button 
                            disabled={!selectedSlotId} 
                            isLoading={intentMutation.isPending}
                            onClick={() => preparePayment({})}
                          >
                            Continue to Payment
                          </Button>
                        </div>
                      </div>
                    )}
                  </CardContent>
                )}
              </Card>
            )}

            <Card className={step === 3 ? 'border-primary shadow-md' : 'opacity-70'}>
              <CardHeader className="flex flex-row items-center justify-between bg-slate-50/50 border-b pb-4">
                <CardTitle className="flex items-center gap-2">
                  <span className="bg-primary text-primary-foreground w-6 h-6 rounded-full flex items-center justify-center text-sm">
                    {orderType === 'STORE_PICKUP' ? '2' : '3'}
                  </span> 
                  Payment
                </CardTitle>
              </CardHeader>
              {step === 3 && clientSecret && (
                <CardContent className="p-6">
                  <Elements stripe={stripePromise} options={{ clientSecret }}>
                    <PaymentForm 
                      clientSecret={clientSecret} 
                      orderPayload={orderPayload} 
                      onSuccess={(data) => {
                        setCompletedOrder(data);
                        clearCart();
                        setStep(4);
                      }}
                      onCancel={() => {
                        setStep(orderType === 'STORE_PICKUP' ? 1 : 2);
                      }}
                    />
                  </Elements>
                </CardContent>
              )}
            </Card>

          </div>
          
          <div>
            <Card className="sticky top-24">
              <CardHeader className="bg-slate-50/50 border-b">
                <CardTitle>Order Summary</CardTitle>
              </CardHeader>
              <CardContent className="p-6 space-y-4">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Items ({cart.items.reduce((a,b)=>a+b.quantity, 0)})</span>
                  <span className="font-medium">${cart.totalAmount.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Taxes</span>
                  <span className="font-medium">$0.00</span>
                </div>
                
                {orderType === 'HOME_DELIVERY' && (
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Delivery Fee</span>
                    <span className="font-medium">$5.00</span>
                  </div>
                )}
                
                <div className="border-t pt-4 mt-4 flex justify-between font-bold text-lg">
                  <span>Total</span>
                  <span className="text-primary">
                    ${(cart.totalAmount + (orderType === 'HOME_DELIVERY' ? 5 : 0)).toFixed(2)}
                  </span>
                </div>
              </CardContent>
            </Card>
          </div>
          
        </div>
      </div>
    </div>
  );
}
