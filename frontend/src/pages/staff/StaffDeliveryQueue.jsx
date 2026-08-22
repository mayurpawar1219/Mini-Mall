import { useState, useEffect } from 'react';
import { staffApi } from '../../api/staffApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Truck, Check } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { format } from 'date-fns';

export default function StaffDeliveryQueue() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const res = await staffApi.getOrders({ page: 0, size: 100 });
      const deliveryOrders = res.data.content?.filter(o => 
        o.type === 'HOME_DELIVERY' && o.status === 'OUT_FOR_DELIVERY'
      ) || [];
      setOrders(deliveryOrders);
    } catch (err) {
      setError('Failed to load delivery orders');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id, status) => {
    try {
      await staffApi.updateOrderStatus(id, status);
      fetchOrders();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Delivery Queue</h1>
        <p className="text-slate-500 mt-2">Manage orders out for home delivery</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle>Delivery Orders</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : orders.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <Truck className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">No active delivery orders</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Order #</th>
                    <th className="px-6 py-3">Created</th>
                    <th className="px-6 py-3">Total</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.id} className="bg-white border-b hover:bg-slate-50">
                      <td className="px-6 py-4 font-medium">{order.orderNumber}</td>
                      <td className="px-6 py-4 text-slate-500">
                        {format(new Date(order.createdAt), 'HH:mm')}
                      </td>
                      <td className="px-6 py-4 font-medium">${order.totalAmount.toFixed(2)}</td>
                      <td className="px-6 py-4">
                        <Badge variant={order.status === 'OUT_FOR_DELIVERY' ? 'warning' : 'default'}>{order.status}</Badge>
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        {order.status === 'OUT_FOR_DELIVERY' && (
                          <Button variant="outline" size="sm" className="text-green-600 border-green-200 hover:bg-green-50" onClick={() => handleUpdateStatus(order.id, 'DELIVERED')}>
                            <Check className="h-4 w-4 mr-1" /> Delivered
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
