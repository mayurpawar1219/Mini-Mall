import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staffApi';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import { Package, Truck, MapPin, Clock, Search, MoreHorizontal } from 'lucide-react';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function StaffDashboard() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL'); // ALL, STORE_PICKUP, HOME_DELIVERY

  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ['staff-stats'],
    queryFn: () => staffApi.getDashboardStats(),
  });

  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['staff-orders'],
    queryFn: () => staffApi.getOrders({ size: 50 }),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ orderId, status }) => staffApi.updateOrderStatus(orderId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff-orders'] });
      queryClient.invalidateQueries({ queryKey: ['staff-stats'] });
      toast.success('Order status updated');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to update status');
    }
  });

  const stats = statsData?.data;
  let orders = ordersData?.data?.content || [];
  
  if (filterType !== 'ALL') {
    orders = orders.filter(o => o.type === filterType);
  }
  
  if (searchTerm) {
    orders = orders.filter(o => o.orderNumber.toLowerCase().includes(searchTerm.toLowerCase()));
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DELIVERED':
      case 'PICKED_UP':
        return <Badge variant="success">Completed</Badge>;
      case 'CANCELLED':
        return <Badge variant="destructive">Cancelled</Badge>;
      case 'PLACED':
      case 'CONFIRMED':
        return <Badge variant="default">New</Badge>;
      case 'PREPARING':
        return <Badge variant="warning">Preparing</Badge>;
      case 'READY_FOR_PICKUP':
      case 'OUT_FOR_DELIVERY':
        return <Badge variant="info">Ready/Out</Badge>;
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  };

  const nextStatusOptions = (currentStatus, type) => {
    const isDelivery = type === 'HOME_DELIVERY';
    switch (currentStatus) {
      case 'PLACED': return ['CONFIRMED'];
      case 'CONFIRMED': return ['PREPARING'];
      case 'PREPARING': return isDelivery ? ['OUT_FOR_DELIVERY'] : ['READY_FOR_PICKUP'];
      case 'READY_FOR_PICKUP': return ['PICKED_UP'];
      case 'OUT_FOR_DELIVERY': return ['DELIVERED'];
      default: return [];
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Staff Operations</h1>
        <p className="text-muted-foreground">Manage orders and store fulfillment.</p>
      </div>
      
      {/* KPI Cards */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {statsLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}><CardContent className="p-6"><Skeleton className="h-16 w-full" /></CardContent></Card>
          ))
        ) : (
          <>
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-4">
                  <div className="p-3 bg-primary/10 text-primary rounded-full">
                    <Package className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Active Orders</p>
                    <h3 className="text-2xl font-bold">{stats?.activeOrders || 0}</h3>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-4">
                  <div className="p-3 bg-amber-100 text-amber-600 rounded-full">
                    <Clock className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Pending Prep</p>
                    <h3 className="text-2xl font-bold">{stats?.pendingPrep || 0}</h3>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-4">
                  <div className="p-3 bg-emerald-100 text-emerald-600 rounded-full">
                    <MapPin className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">To Pickup Today</p>
                    <h3 className="text-2xl font-bold">{stats?.pickupToday || 0}</h3>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-4">
                  <div className="p-3 bg-blue-100 text-blue-600 rounded-full">
                    <Truck className="h-6 w-6" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">To Deliver Today</p>
                    <h3 className="text-2xl font-bold">{stats?.deliveryToday || 0}</h3>
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        )}
      </div>

      {/* Orders Table */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row items-start sm:items-center justify-between border-b bg-slate-50/50 pb-4 gap-4">
          <CardTitle>Recent Orders</CardTitle>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm"
                placeholder="Search order number..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <select 
              className="flex h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
            >
              <option value="ALL">All Types</option>
              <option value="HOME_DELIVERY">Delivery Only</option>
              <option value="STORE_PICKUP">Pickup Only</option>
            </select>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-muted-foreground uppercase bg-slate-50 border-b">
                <tr>
                  <th className="px-6 py-4 font-medium">Order Number</th>
                  <th className="px-6 py-4 font-medium">Date</th>
                  <th className="px-6 py-4 font-medium">Type</th>
                  <th className="px-6 py-4 font-medium">Status</th>
                  <th className="px-6 py-4 font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {ordersLoading ? (
                  <tr>
                    <td colSpan="5" className="px-6 py-8 text-center text-muted-foreground">Loading orders...</td>
                  </tr>
                ) : orders.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-6 py-8 text-center text-muted-foreground">No orders found.</td>
                  </tr>
                ) : (
                  orders.map(order => {
                    const nextStatuses = nextStatusOptions(order.status, order.type);
                    return (
                      <tr key={order.id} className="bg-white hover:bg-slate-50 transition-colors">
                        <td className="px-6 py-4 font-medium">{order.orderNumber}</td>
                        <td className="px-6 py-4 text-muted-foreground">{format(new Date(order.createdAt), 'MMM d, h:mm a')}</td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            {order.type === 'HOME_DELIVERY' ? <Truck className="h-4 w-4 text-blue-500" /> : <MapPin className="h-4 w-4 text-emerald-500" />}
                            {order.type.replace('_', ' ')}
                          </div>
                        </td>
                        <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                        <td className="px-6 py-4">
                          {nextStatuses.length > 0 ? (
                            <Button 
                              size="sm" 
                              onClick={() => updateStatusMutation.mutate({ orderId: order.id, status: nextStatuses[0] })}
                              isLoading={updateStatusMutation.isPending}
                            >
                              Mark {nextStatuses[0].replace(/_/g, ' ')}
                            </Button>
                          ) : (
                            <span className="text-muted-foreground">-</span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
