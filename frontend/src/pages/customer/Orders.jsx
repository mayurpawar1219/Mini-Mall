import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '../../api/orderApi';
import { Card, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import { Package, Truck, Clock, MapPin, Search, XCircle } from 'lucide-react';
import { format } from 'date-fns';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

export default function Orders() {
  const [searchTerm, setSearchTerm] = useState('');
  const queryClient = useQueryClient();

  const { data: ordersData, isLoading, error } = useQuery({
    queryKey: ['my-orders'],
    queryFn: () => orderApi.getMyOrders(),
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId) => orderApi.cancelOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-orders'] });
      toast.success('Order cancelled successfully');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to cancel order');
    },
  });

  const handleCancelOrder = (orderId, orderNumber) => {
    if (window.confirm(`Are you sure you want to cancel Order #${orderNumber}?`)) {
      cancelMutation.mutate(orderId);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DELIVERED':
      case 'PICKED_UP':
        return <Badge variant="success">Completed</Badge>;
      case 'CANCELLED':
        return <Badge variant="destructive">Cancelled</Badge>;
      case 'READY_FOR_PICKUP':
      case 'OUT_FOR_DELIVERY':
        return <Badge variant="info">Ready</Badge>;
      case 'PLACED':
        return <Badge variant="secondary">Placed</Badge>;
      case 'CONFIRMED':
        return <Badge className="bg-blue-100 text-blue-800">Confirmed</Badge>;
      case 'PREPARING':
        return <Badge className="bg-amber-100 text-amber-800">Preparing</Badge>;
      default:
        return <Badge variant="secondary">{status?.replace(/_/g, ' ')}</Badge>;
    }
  };

  const isCancellable = (status) => {
    return status === 'PLACED' || status === 'CONFIRMED';
  };

  const getOrderIcon = (type) => {
    if (type === 'HOME_DELIVERY') return <Truck className="h-5 w-5 text-blue-500" />;
    if (type === 'STORE_PICKUP') return <MapPin className="h-5 w-5 text-emerald-500" />;
    return <Clock className="h-5 w-5 text-amber-500" />;
  };

  const orders = ordersData?.data?.content || ordersData?.data || [];
  const filteredOrders = orders.filter(order =>
    order.orderNumber?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (error) {
    return (
      <div className="container py-20 text-center">
        <div className="text-destructive mb-4">
          <XCircle className="h-12 w-12 mx-auto" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Failed to load orders</h2>
        <p className="text-muted-foreground mb-6">{error.response?.data?.message || 'An unexpected error occurred.'}</p>
        <Button onClick={() => queryClient.invalidateQueries({ queryKey: ['my-orders'] })}>
          Try Again
        </Button>
      </div>
    );
  }

  return (
    <div className="container py-8 px-4 md:px-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">My Orders</h1>
          <p className="text-muted-foreground">View and manage your recent orders.</p>
        </div>
      </div>

      <div className="grid gap-6">
        <div className="relative w-full max-w-sm">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            placeholder="Search by order number..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map(i => (
              <Card key={i}>
                <CardContent className="p-6">
                  <div className="flex justify-between items-center mb-4">
                    <Skeleton className="h-6 w-32" />
                    <Skeleton className="h-6 w-20" />
                  </div>
                  <Skeleton className="h-4 w-48 mb-2" />
                  <Skeleton className="h-4 w-32" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="text-center py-20 bg-slate-50 rounded-xl border border-dashed">
            <Package className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-slate-900 mb-1">No orders found</h3>
            <p className="text-muted-foreground mb-4">
              {searchTerm ? 'No orders match your search.' : "You haven't placed any orders yet."}
            </p>
            <Link to="/products">
              <Button>Start Shopping</Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map(order => (
              <Card key={order.id} className="overflow-hidden hover:shadow-md transition-shadow">
                <CardContent className="p-0">
                  <div className="flex flex-col md:flex-row md:items-center justify-between p-6 bg-slate-50 border-b">
                    <div className="flex items-start gap-4">
                      <div className="p-3 bg-white rounded-full border shadow-sm mt-1">
                        {getOrderIcon(order.type)}
                      </div>
                      <div>
                        <div className="flex items-center gap-3 mb-1">
                          <h3 className="font-semibold text-lg">Order #{order.orderNumber}</h3>
                          {getStatusBadge(order.status)}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          Placed on {format(new Date(order.createdAt), 'MMM d, yyyy h:mm a')}
                        </div>
                      </div>
                    </div>
                    <div className="mt-4 md:mt-0 text-left md:text-right">
                      <div className="font-bold text-xl mb-1">${order.totalAmount?.toFixed(2)}</div>
                      <div className="text-sm text-muted-foreground">{order.items?.length || order.itemCount || 0} items</div>
                    </div>
                  </div>

                  <div className="p-6 flex flex-col md:flex-row justify-between items-center gap-4">
                    <div className="flex -space-x-2 overflow-hidden w-full md:w-auto">
                      {order.items?.slice(0, 5).map((item, idx) => (
                        <div key={idx} className="inline-flex h-10 w-10 rounded-full bg-slate-200 border-2 border-white items-center justify-center font-bold text-slate-500 text-xs shadow-sm" style={{ zIndex: 5 - idx }}>
                          {item.productName?.charAt(0) || '?'}
                        </div>
                      ))}
                      {order.items?.length > 5 && (
                        <div className="inline-flex h-10 w-10 rounded-full bg-slate-100 border-2 border-white items-center justify-center font-bold text-slate-500 text-xs shadow-sm z-0">
                          +{order.items.length - 5}
                        </div>
                      )}
                    </div>

                    <div className="flex gap-2 w-full md:w-auto">
                      {isCancellable(order.status) && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-destructive border-destructive/30 hover:bg-destructive/10"
                          onClick={() => handleCancelOrder(order.id, order.orderNumber)}
                          disabled={cancelMutation.isPending}
                        >
                          Cancel
                        </Button>
                      )}
                      <Link to={`/orders/${order.id}`} className="flex-1 md:flex-none">
                        <Button variant="outline" className="w-full">View Details</Button>
                      </Link>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
