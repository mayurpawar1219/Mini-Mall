import { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { ShoppingBag, Eye, Truck, Store, CalendarClock, X } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { format } from 'date-fns';

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getOrders({ page: 0, size: 50 });
      setOrders(res.data.content || []);
    } catch (err) {
      setError('Failed to load orders');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewOrder = async (id) => {
    try {
      setModalLoading(true);
      const res = await adminApi.getOrder(id);
      setSelectedOrder(res.data);
    } catch (err) {
      console.error('Failed to fetch order details', err);
    } finally {
      setModalLoading(false);
    }
  };

  const closeModal = () => setSelectedOrder(null);

  const getStatusBadge = (status) => {
    const variants = {
      PLACED: 'default',
      CONFIRMED: 'default',
      PREPARING: 'warning',
      READY_FOR_PICKUP: 'warning',
      OUT_FOR_DELIVERY: 'warning',
      DELIVERED: 'success',
      PICKED_UP: 'success',
      CANCELLED: 'destructive',
    };
    return <Badge variant={variants[status] || 'secondary'}>{status}</Badge>;
  };

  const getTypeIcon = (type) => {
    if (type === 'HOME_DELIVERY') return <Truck className="h-4 w-4 text-blue-500" title="Home Delivery" />;
    if (type === 'STORE_PICKUP') return <Store className="h-4 w-4 text-emerald-500" title="Store Pickup" />;
    return <CalendarClock className="h-4 w-4 text-purple-500" title="Scheduled Pickup" />;
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Order Management</h1>
        <p className="text-slate-500 mt-2">View and manage all customer orders</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle>Recent Orders</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : orders.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <ShoppingBag className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">No orders found</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Order Number</th>
                    <th className="px-6 py-3">Date</th>
                    <th className="px-6 py-3">Type</th>
                    <th className="px-6 py-3">Items</th>
                    <th className="px-6 py-3">Total</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.id} className="bg-white border-b hover:bg-slate-50">
                      <td className="px-6 py-4 font-medium text-slate-900">
                        {order.orderNumber}
                      </td>
                      <td className="px-6 py-4 text-slate-500">
                        {format(new Date(order.createdAt), 'MMM dd, yyyy HH:mm')}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center space-x-2">
                          {getTypeIcon(order.type)}
                          <span>{order.type.replace('_', ' ')}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4">{order.itemCount}</td>
                      <td className="px-6 py-4 font-medium">
                        ₹{(order.totalAmount || 0).toFixed(2)}
                      </td>
                      <td className="px-6 py-4">
                        {getStatusBadge(order.status)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Button variant="ghost" size="sm" className="text-blue-600 hover:text-blue-800" onClick={() => handleViewOrder(order.id)}>
                          <Eye className="h-4 w-4 mr-1" /> View
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Order Details Modal */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
            <div className="px-6 py-4 border-b flex justify-between items-center bg-slate-50">
              <h2 className="text-xl font-bold flex items-center gap-2">
                Order {selectedOrder.orderNumber}
                {getStatusBadge(selectedOrder.status)}
              </h2>
              <button onClick={closeModal} className="text-slate-400 hover:text-slate-600">
                <X className="h-5 w-5" />
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto space-y-6">
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">Customer Details</h3>
                  <p className="font-medium text-slate-900">{selectedOrder.customerName}</p>
                  <p className="text-slate-600">{selectedOrder.customerEmail}</p>
                </div>
                
                <div>
                  <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">Order Info</h3>
                  <p><span className="text-slate-500">Date:</span> {format(new Date(selectedOrder.createdAt), 'MMM dd, yyyy HH:mm')}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-slate-500">Type:</span> 
                    {getTypeIcon(selectedOrder.type)}
                    <span>{selectedOrder.type.replace('_', ' ')}</span>
                  </div>
                </div>
              </div>

              {selectedOrder.deliveryAddress && (
                <div>
                  <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">Delivery Address</h3>
                  <p className="text-slate-700 bg-slate-50 p-3 rounded border border-slate-100">
                    {selectedOrder.deliveryAddress}<br/>
                    {selectedOrder.deliveryCity} {selectedOrder.deliveryPostalCode}
                  </p>
                </div>
              )}
              
              {selectedOrder.pickupSlotId && (
                <div>
                  <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">Pickup Slot</h3>
                  <p className="text-slate-700 bg-slate-50 p-3 rounded border border-slate-100">
                    {format(new Date(selectedOrder.pickupSlotStartTime), 'MMM dd, yyyy - HH:mm')} to {format(new Date(selectedOrder.pickupSlotEndTime), 'HH:mm')}
                  </p>
                </div>
              )}

              <div>
                <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">Order Items</h3>
                <div className="border rounded-lg overflow-hidden">
                  <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 border-b">
                      <tr>
                        <th className="px-4 py-2 font-medium text-slate-500">Product</th>
                        <th className="px-4 py-2 font-medium text-slate-500 text-center">Qty</th>
                        <th className="px-4 py-2 font-medium text-slate-500 text-right">Price</th>
                        <th className="px-4 py-2 font-medium text-slate-500 text-right">Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {selectedOrder.items.map((item) => (
                        <tr key={item.id}>
                          <td className="px-4 py-3">
                            <div className="font-medium text-slate-900">{item.productName}</div>
                          </td>
                          <td className="px-4 py-3 text-center">{item.quantity}</td>
                          <td className="px-4 py-3 text-right">₹{(item.priceAtPurchase || 0).toFixed(2)}</td>
                          <td className="px-4 py-3 text-right font-medium">₹{(item.quantity * (item.priceAtPurchase || 0)).toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="bg-slate-50 font-medium">
                      <tr>
                        <td colSpan="3" className="px-4 py-3 text-right">Total Amount:</td>
                        <td className="px-4 py-3 text-right text-lg text-primary">₹{(selectedOrder.totalAmount || 0).toFixed(2)}</td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>
            </div>
            
            <div className="px-6 py-4 border-t bg-slate-50 flex justify-end">
              <Button onClick={closeModal} variant="outline">Close</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
