import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { orderApi } from '../../api/orderApi';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import {
  ShoppingBag, Package, UserCircle, ArrowRight,
  ShoppingCart, Truck, MapPin, Clock, TrendingUp
} from 'lucide-react';
import { format } from 'date-fns';

export default function CustomerDashboard() {
  const { user } = useAuth();
  const { cart } = useCart();

  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['my-orders'],
    queryFn: () => orderApi.getMyOrders(),
  });

  const orders = ordersData?.data?.content || ordersData?.data || [];
  const recentOrders = orders.slice(0, 3);
  const cartItemCount = cart?.items?.length || 0;
  const cartTotal = cart?.totalAmount || 0;

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DELIVERED':
      case 'PICKED_UP':
        return <Badge variant="success">Completed</Badge>;
      case 'CANCELLED':
        return <Badge variant="destructive">Cancelled</Badge>;
      case 'PLACED':
        return <Badge variant="secondary">Placed</Badge>;
      default:
        return <Badge variant="secondary">{status?.replace(/_/g, ' ')}</Badge>;
    }
  };

  return (
    <div className="container py-8 px-4 md:px-6">
      {/* Welcome Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight">
          Welcome back, {user?.firstName}! 👋
        </h1>
        <p className="text-muted-foreground mt-1">
          Here's a quick overview of your account.
        </p>
      </div>

      {/* Quick Action Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <Link to="/products">
          <Card className="hover:shadow-md hover:border-primary/50 transition-all cursor-pointer h-full">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="p-3 bg-emerald-100 rounded-xl">
                <ShoppingBag className="h-6 w-6 text-emerald-600" />
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Browse</div>
                <div className="font-semibold">Products</div>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link to="/checkout">
          <Card className="hover:shadow-md hover:border-primary/50 transition-all cursor-pointer h-full">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="p-3 bg-blue-100 rounded-xl relative">
                <ShoppingCart className="h-6 w-6 text-blue-600" />
                {cartItemCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-primary text-primary-foreground text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                    {cartItemCount}
                  </span>
                )}
              </div>
              <div>
                <div className="text-sm text-muted-foreground">My Cart</div>
                <div className="font-semibold">
                  {cartItemCount > 0 ? `$${cartTotal.toFixed(2)}` : 'Empty'}
                </div>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link to="/orders">
          <Card className="hover:shadow-md hover:border-primary/50 transition-all cursor-pointer h-full">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="p-3 bg-purple-100 rounded-xl">
                <Package className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <div className="text-sm text-muted-foreground">My Orders</div>
                <div className="font-semibold">{orders.length} orders</div>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link to="/profile">
          <Card className="hover:shadow-md hover:border-primary/50 transition-all cursor-pointer h-full">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="p-3 bg-amber-100 rounded-xl">
                <UserCircle className="h-6 w-6 text-amber-600" />
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Account</div>
                <div className="font-semibold">Profile</div>
              </div>
            </CardContent>
          </Card>
        </Link>
      </div>

      {/* Recent Orders */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between bg-slate-50/50 border-b">
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-muted-foreground" />
            Recent Orders
          </CardTitle>
          <Link to="/orders">
            <Button variant="ghost" size="sm">
              View All <ArrowRight className="ml-1 h-4 w-4" />
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          {ordersLoading ? (
            <div className="p-6 space-y-4">
              {[1, 2, 3].map(i => (
                <div key={i} className="flex justify-between items-center">
                  <Skeleton className="h-5 w-32" />
                  <Skeleton className="h-5 w-20" />
                </div>
              ))}
            </div>
          ) : recentOrders.length === 0 ? (
            <div className="p-12 text-center">
              <Package className="h-10 w-10 text-slate-300 mx-auto mb-3" />
              <h3 className="font-medium text-slate-700 mb-1">No orders yet</h3>
              <p className="text-sm text-muted-foreground mb-4">
                Start shopping to see your orders here.
              </p>
              <Link to="/products">
                <Button size="sm">Browse Products</Button>
              </Link>
            </div>
          ) : (
            <div className="divide-y">
              {recentOrders.map(order => (
                <Link
                  key={order.id}
                  to={`/orders/${order.id}`}
                  className="flex flex-col sm:flex-row sm:items-center justify-between p-4 sm:p-6 hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center gap-4 mb-2 sm:mb-0">
                    <div className="p-2 bg-slate-100 rounded-full">
                      {order.type === 'HOME_DELIVERY' ? (
                        <Truck className="h-4 w-4 text-blue-500" />
                      ) : (
                        <MapPin className="h-4 w-4 text-emerald-500" />
                      )}
                    </div>
                    <div>
                      <div className="font-medium">Order #{order.orderNumber}</div>
                      <div className="text-sm text-muted-foreground">
                        {format(new Date(order.createdAt), 'MMM d, yyyy')}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 pl-12 sm:pl-0">
                    {getStatusBadge(order.status)}
                    <span className="font-bold">${order.totalAmount?.toFixed(2)}</span>
                    <ArrowRight className="h-4 w-4 text-muted-foreground hidden sm:block" />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
