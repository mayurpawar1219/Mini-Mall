import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LogOut, PackageSearch, LayoutDashboard, ShoppingBag, MapPin, Truck, Repeat2, ArrowLeftRight, Package } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Button } from '../ui/Button';

export default function StaffLayout() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/auth/login');
  };

  const navItems = [
    { name: 'Dashboard', path: '/staff', icon: LayoutDashboard },
    { name: 'Orders', path: '/staff/orders', icon: ShoppingBag },
    { name: 'Store Pickup', path: '/staff/pickup', icon: MapPin },
    { name: 'Delivery', path: '/staff/delivery', icon: Truck },
    { name: 'Returns', path: '/staff/returns', icon: Repeat2 },
    { name: 'Exchanges', path: '/staff/exchanges', icon: ArrowLeftRight },
    { name: 'Inventory', path: '/staff/inventory', icon: Package },
  ];

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 z-50 w-64 border-r bg-white flex flex-col hidden md:flex">
        <div className="flex h-16 items-center border-b px-6">
          <Link to="/staff" className="flex items-center gap-2 font-bold text-xl text-primary">
            <PackageSearch className="h-6 w-6" />
            Mini D-Mart
          </Link>
        </div>
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path || (item.path !== '/staff' && location.pathname.startsWith(item.path));
            return (
              <Link
                key={item.name}
                to={item.path}
                className={cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                  isActive 
                    ? "bg-primary text-primary-foreground" 
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                )}
              >
                <item.icon className="h-5 w-5" />
                {item.name}
              </Link>
            );
          })}
        </nav>
        <div className="border-t p-4">
          <Button variant="ghost" className="w-full justify-start text-slate-600" onClick={handleLogout}>
            <LogOut className="mr-2 h-5 w-5" />
            Logout
          </Button>
        </div>
      </aside>

      <main className="flex-1 md:ml-64 flex flex-col min-h-screen">
        <header className="h-16 border-b bg-white flex items-center justify-between px-6 md:hidden">
          <span className="font-bold text-primary">Staff Operations</span>
          <Button variant="ghost" size="icon" onClick={handleLogout}><LogOut className="h-5 w-5"/></Button>
        </header>
        <div className="p-6 md:p-8 flex-1">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
