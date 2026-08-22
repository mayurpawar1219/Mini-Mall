import { useState, useEffect } from 'react';
import { staffApi } from '../../api/staffApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { ShoppingBag, CheckCircle, Package } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { format } from 'date-fns';

export default function StaffOrderPreparation() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      // Fetch PREPARING or CONFIRMED orders
      const res = await staffApi.getOrders({ page: 0, size: 100 });
      const prepOrders = res.data.content?.filter(o => o.status === 'CONFIRMED' || o.status === 'PREPARING') || [];
      setOrders(prepOrders);
    } catch (err) {
      setError('Failed to load orders for preparation');
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
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Order Preparation</h1>
        <p className="text-slate-500 mt-2">Manage orders that need to be picked and packed</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle>Preparation Queue</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : orders.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <Package className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">Queue is empty</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Order Number</th>
                    <th className="px-6 py-3">Date</th>
                    <th className="px-6 py-3">Type</th>
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
                      <td className="px-6 py-4">{order.type.replace('_', ' ')}</td>
                      <td className="px-6 py-4">
                        <Badge variant={order.status === 'PREPARING' ? 'warning' : 'default'}>{order.status}</Badge>
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        {order.status === 'CONFIRMED' && (
                          <Button variant="outline" size="sm" onClick={() => handleUpdateStatus(order.id, 'PREPARING')}>
                            Start Prep
                          </Button>
                        )}
                        {order.status === 'PREPARING' && (
                          <Button 
                            variant="outline" 
                            size="sm" 
                            className="text-emerald-600 border-emerald-200 hover:bg-emerald-50" 
                            onClick={() => handleUpdateStatus(order.id, order.type === 'HOME_DELIVERY' ? 'OUT_FOR_DELIVERY' : 'READY_FOR_PICKUP')}
                          >
                            <CheckCircle className="h-4 w-4 mr-1" /> {order.type === 'HOME_DELIVERY' ? 'Dispatch' : 'Mark Ready'}
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
