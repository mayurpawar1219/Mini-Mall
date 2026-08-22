import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import CartDrawer from '../cart/CartDrawer';

export default function MainLayout() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <CartDrawer />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t bg-white py-6 md:py-0">
        <div className="container flex flex-col items-center justify-between gap-4 md:h-16 md:flex-row text-sm text-muted-foreground">
          <p>Built for Mini D-Mart. Premium Grocery Experience.</p>
        </div>
      </footer>
    </div>
  );
}
