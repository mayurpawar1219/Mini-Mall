import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '../../api/orderApi';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import { ArrowLeft, MapPin, Truck, CalendarClock, Package, CheckCircle2, XCircle } from 'lucide-react';
import { format } from 'date-fns';
import { cn } from '../../lib/utils';
import toast from 'react-hot-toast';

export default function OrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: orderData, isLoading, error } = useQuery({
    queryKey: ['order', id],
    queryFn: () => orderApi.getOrderById(id),
  });

  const cancelMutation = useMutation({
    mutationFn: () => orderApi.cancelOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] });
      queryClient.invalidateQueries({ queryKey: ['my-orders'] });
      toast.success('Order cancelled successfully');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to cancel order');
    },
  });

  const handleCancel = () => {
    if (window.confirm('Are you sure you want to cancel this order?')) {
      cancelMutation.mutate();
    }
  };

  if (isLoading) {
    return (
      <div className="container py-8 px-4 md:px-6 space-y-6">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-32 w-full rounded-xl" />
        <div className="grid md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-4">
            <Skeleton className="h-64 w-full rounded-xl" />
          </div>
          <Skeleton className="h-64 w-full rounded-xl" />
        </div>
      </div>
    );
  }

  if (error) {
    const is404 = error.response?.status === 404;
    const is403 = error.response?.status === 403;
    return (
      <div className="container py-20 text-center">
        <XCircle className="h-12 w-12 text-destructive mx-auto mb-4" />
        <h2 className="text-2xl font-bold mb-2">
          {is404 ? 'Order Not Found' : is403 ? 'Access Denied' : 'Error Loading Order'}
        </h2>
        <p className="text-muted-foreground mb-6">
          {is404 ? "This order doesn't exist or doesn't belong to your account." :
           is403 ? "You don't have permission to view this order." :
           error.response?.data?.message || 'An unexpected error occurred.'}
        </p>
        <Link to="/orders"><Button>Back to Orders</Button></Link>
      </div>
    );
  }

  const order = orderData?.data;

  if (!order) {
    return (
      <div className="container py-20 text-center">
        <h2 className="text-2xl font-bold mb-2">Order Not Found</h2>
        <Link to="/orders"><Button>Back to Orders</Button></Link>
      </div>
    );
  }

  const isCancellable = order.status === 'PLACED' || order.status === 'CONFIRMED';
  const isCancelled = order.status === 'CANCELLED';

  const getStatusIndex = (status) => {
    if (status === 'CANCELLED') return -1;
    if (status === 'READY_FOR_PICKUP' || status === 'OUT_FOR_DELIVERY') return 3;
    if (status === 'DELIVERED' || status === 'PICKED_UP') return 4;
    const statuses = ['PLACED', 'CONFIRMED', 'PREPARING'];
    const idx = statuses.indexOf(status);
    return idx === -1 ? 0 : idx;
  };

  const currentStep = getStatusIndex(order.status);

  const steps = [
    { label: 'Placed', icon: Package },
    { label: 'Confirmed', icon: CheckCircle2 },
    { label: 'Preparing', icon: Package },
    { label: order.type === 'HOME_DELIVERY' ? 'Out for Delivery' : 'Ready for Pickup', icon: order.type === 'HOME_DELIVERY' ? Truck : MapPin },
    { label: order.type === 'HOME_DELIVERY' ? 'Delivered' : 'Picked Up', icon: CheckCircle2 },
  ];

  return (
    <div className="container py-8 px-4 md:px-6">
      <Link to="/orders" className="inline-flex items-center text-sm font-medium text-muted-foreground hover:text-primary mb-6 transition-colors">
        <ArrowLeft className="mr-2 h-4 w-4" /> Back to Orders
      </Link>

      <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight mb-2">Order #{order.orderNumber}</h1>
          <p className="text-muted-foreground">
            Placed on {format(new Date(order.createdAt), 'MMMM d, yyyy h:mm a')}
          </p>
        </div>
        <div className="flex items-center gap-3">
          {isCancellable && (
            <Button
              variant="outline"
              className="text-destructive border-destructive/30 hover:bg-destructive/10"
              onClick={handleCancel}
              disabled={cancelMutation.isPending}
            >
              {cancelMutation.isPending ? 'Cancelling...' : 'Cancel Order'}
            </Button>
          )}
          {isCancelled ? (
            <Badge variant="destructive" className="text-sm px-3 py-1">Cancelled</Badge>
          ) : (
            <Badge variant="secondary" className="text-sm px-3 py-1">{order.status.replace(/_/g, ' ')}</Badge>
          )}
        </div>
      </div>

      {/* Visual Timeline Stepper */}
      {!isCancelled && (
        <Card className="mb-8 overflow-hidden">
          <CardContent className="p-8">
            <div className="relative">
              <div className="absolute top-1/2 left-0 w-full h-1 bg-slate-200 -translate-y-1/2 rounded-full hidden sm:block"></div>
              <div
                className="absolute top-1/2 left-0 h-1 bg-primary -translate-y-1/2 rounded-full hidden sm:block transition-all duration-500"
                style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}
              ></div>

              <div className="relative flex flex-col sm:flex-row justify-between gap-6 sm:gap-0">
                {steps.map((step, idx) => {
                  const isActive = currentStep >= idx;
                  const isCurrent = currentStep === idx;

                  return (
                    <div key={idx} className="flex flex-row sm:flex-col items-center gap-4 sm:gap-2 z-10">
                      <div className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center border-2 bg-white transition-colors duration-300",
                        isActive ? "border-primary text-primary" : "border-slate-200 text-slate-300",
                        isCurrent ? "bg-primary text-primary-foreground ring-4 ring-primary/20" : ""
                      )}>
                        <step.icon className="w-5 h-5" />
                      </div>
                      <div className="sm:text-center">
                        <div className={cn("text-sm font-semibold", isActive ? "text-slate-900" : "text-muted-foreground")}>{step.label}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {isCancelled && (
        <Card className="mb-8 border-destructive/30 bg-destructive/5">
          <CardContent className="p-6 flex items-center gap-4">
            <XCircle className="h-8 w-8 text-destructive flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-destructive">Order Cancelled</h3>
              <p className="text-sm text-muted-foreground">This order has been cancelled.</p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <Card>
            <CardHeader className="bg-slate-50/50 border-b">
              <CardTitle>Items Ordered</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y">
                {order.items?.map(item => (
                  <div key={item.id} className="flex gap-4 p-6">
                    <div className="w-16 h-16 bg-slate-100 rounded-md flex items-center justify-center font-bold text-slate-400 flex-shrink-0">
                      {item.productName?.charAt(0) || '?'}
                    </div>
                    <div className="flex-1">
                      <div className="flex justify-between items-start">
                        <h4 className="font-semibold">{item.productName}</h4>
                        <div className="font-bold">${item.subtotal?.toFixed(2)}</div>
                      </div>
                      <div className="text-sm text-muted-foreground mt-1">
                        ${item.priceAtPurchase?.toFixed(2)} × {item.quantity}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card>
            <CardHeader className="bg-slate-50/50 border-b">
              <CardTitle>Order Summary</CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-4">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Subtotal</span>
                <span className="font-medium">${order.totalAmount?.toFixed(2)}</span>
              </div>
              <div className="border-t pt-4 flex justify-between font-bold text-lg">
                <span>Total</span>
                <span className="text-primary">${order.totalAmount?.toFixed(2)}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="bg-slate-50/50 border-b">
              <CardTitle>Fulfillment Information</CardTitle>
            </CardHeader>
            <CardContent className="p-6">
              <div className="flex items-center gap-2 font-medium mb-2">
                {order.type === 'HOME_DELIVERY' ? <Truck className="w-4 h-4 text-blue-500" /> : <MapPin className="w-4 h-4 text-emerald-500" />}
                {order.type?.replace(/_/g, ' ')}
              </div>

              <div className="text-sm text-muted-foreground mt-4">
                {order.type === 'HOME_DELIVERY' && order.deliveryAddress && (
                  <div>
                    <div className="font-medium text-foreground mb-1">Delivery Address</div>
                    <p>{order.deliveryAddress.addressLine1}</p>
                    {order.deliveryAddress.addressLine2 && <p>{order.deliveryAddress.addressLine2}</p>}
                    <p>{order.deliveryAddress.city}, {order.deliveryAddress.postalCode}</p>
                  </div>
                )}

                {order.type === 'SCHEDULED_PICKUP' && order.pickupSlot && (
                  <div>
                    <div className="font-medium text-foreground mb-1">Pickup Time</div>
                    <p>{order.pickupSlot.slotDate}</p>
                    <p>{order.pickupSlot.startTime?.substring(0,5)} - {order.pickupSlot.endTime?.substring(0,5)}</p>
                  </div>
                )}

                {order.type === 'STORE_PICKUP' && (
                  <div>
                    <div className="font-medium text-foreground mb-1">Store Pickup</div>
                    <p>Visit the store to collect your order.</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
