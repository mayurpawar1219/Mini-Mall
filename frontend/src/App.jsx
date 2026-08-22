import { RouterProvider, createBrowserRouter, Navigate } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';

import MainLayout from './components/layout/MainLayout';
import AuthLayout from './components/layout/AuthLayout';
import AdminLayout from './components/layout/AdminLayout';
import StaffLayout from './components/layout/StaffLayout';

// Pages
import AuthLanding from './pages/auth/AuthLanding';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Home from './pages/customer/Home';
import Products from './pages/customer/Products';
import ProductDetail from './pages/customer/ProductDetail';
import Checkout from './pages/customer/Checkout';
import CustomerDashboard from './pages/customer/CustomerDashboard';
import Orders from './pages/customer/Orders';
import OrderDetail from './pages/customer/OrderDetail';
import Profile from './pages/customer/Profile';

import StaffDashboard from './pages/staff/StaffDashboard';
import StaffOrderPreparation from './pages/staff/StaffOrderPreparation';
import StaffPickupQueue from './pages/staff/StaffPickupQueue';
import StaffDeliveryQueue from './pages/staff/StaffDeliveryQueue';
import StaffReturns from './pages/staff/StaffReturns';
import StaffExchanges from './pages/staff/StaffExchanges';
import StaffInventory from './pages/staff/StaffInventory';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminProducts from './pages/admin/AdminProducts';
import AddProduct from './pages/admin/AddProduct';
import EditProduct from './pages/admin/EditProduct';
import AdminPickupSlots from './pages/admin/AdminPickupSlots';
import AdminOrders from './pages/admin/AdminOrders';
import AdminUsers from './pages/admin/AdminUsers';
import AdminReturns from './pages/admin/AdminReturns';
import AdminExchanges from './pages/admin/AdminExchanges';
import AdminCategories from './pages/admin/AdminCategories';
import AdminAuditLogs from './pages/admin/AdminAuditLogs';
import Unauthorized from './pages/auth/Unauthorized';

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />, 
    children: [
      { index: true, element: <Home /> },
      { path: 'products', element: <Products /> },
      { path: 'products/:id', element: <ProductDetail /> },
      {
        element: <ProtectedRoute allowedRoles={['CUSTOMER']} />,
        children: [
          { path: 'checkout', element: <Checkout /> },
          { path: 'dashboard', element: <CustomerDashboard /> },
          { path: 'orders', element: <Orders /> },
          { path: 'orders/:id', element: <OrderDetail /> },
          { path: 'profile', element: <Profile /> },
          // Keep backward compatibility for old dashboard/orders/:id links
          { path: 'dashboard/orders/:id', element: <OrderDetail /> },
        ]
      },
      { path: 'unauthorized', element: <Unauthorized /> }
    ]
  },
  {
    path: '/auth',
    element: <AuthLayout />,
    children: [
      { index: true, element: <AuthLanding /> },
      { path: 'login/:roleType', element: <Login /> },
      { path: 'register/:roleType', element: <Register /> },
      { path: 'login', element: <Navigate to="/auth" replace /> },
      { path: 'register', element: <Navigate to="/auth" replace /> },
    ]
  },
  {
    path: '/admin',
    element: <ProtectedRoute allowedRoles={['ADMIN']} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <AdminDashboard /> },
          { path: 'products', element: <AdminProducts /> },
          { path: 'products/add', element: <AddProduct /> },
          { path: 'products/edit/:id', element: <EditProduct /> },
          { path: 'pickup-slots', element: <AdminPickupSlots /> },
          { path: 'orders', element: <AdminOrders /> },
          { path: 'users', element: <AdminUsers /> },
          { path: 'returns', element: <AdminReturns /> },
          { path: 'exchanges', element: <AdminExchanges /> },
          { path: 'categories', element: <AdminCategories /> },
          { path: 'audit-logs', element: <AdminAuditLogs /> },
        ]
      }
    ]
  },
  {
    path: '/staff',
    element: <ProtectedRoute allowedRoles={['STAFF']} />,
    children: [
      {
        element: <StaffLayout />,
        children: [
          { index: true, element: <StaffDashboard /> },
          { path: 'orders', element: <StaffOrderPreparation /> },
          { path: 'pickup', element: <StaffPickupQueue /> },
          { path: 'delivery', element: <StaffDeliveryQueue /> },
          { path: 'returns', element: <StaffReturns /> },
          { path: 'exchanges', element: <StaffExchanges /> },
          { path: 'inventory', element: <StaffInventory /> },
        ]
      }
    ]
  },
  {
    path: '*',
    element: <MainLayout />,
    children: [
      { path: '*', element: <Unauthorized /> }
    ]
  }
]);

function App() {
  return <RouterProvider router={router} />;
}

export default App;
