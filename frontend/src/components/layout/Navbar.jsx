import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCartUI } from '../../context/CartUIContext';
import { useCart } from '../../context/CartContext';
import { getRoleDisplayName } from '../../lib/authUtils';
import {
  ShoppingCart, LogOut, PackageSearch, UserCircle,
  Menu, X, ShoppingBag, Package, LayoutDashboard
} from 'lucide-react';
import { Button } from '../ui/Button';

export default function Navbar() {
  const { isAuthenticated, user, role, logout } = useAuth();
  const { openCart } = useCartUI();
  const { cart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isAuthPage = location.pathname.startsWith('/auth');
  const cartItemCount = cart?.items?.length || 0;

  const handleLogout = () => {
    logout();
    setMobileMenuOpen(false);
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  const navLinkClass = (path) =>
    `text-sm font-medium transition-colors ${
      isActive(path)
        ? 'text-primary'
        : 'text-muted-foreground hover:text-foreground'
    }`;

  return (
    <nav className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        {/* Logo */}
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center space-x-2">
            <PackageSearch className="h-6 w-6 text-primary" />
            <span className="inline-block font-bold text-xl text-primary">Mini D-Mart</span>
          </Link>

          {/* Desktop Nav Links */}
          {isAuthenticated && role === 'CUSTOMER' && (
            <div className="hidden md:flex items-center gap-5">
              <Link to="/products" className={navLinkClass('/products')}>
                Products
              </Link>
              <Link to="/orders" className={navLinkClass('/orders')}>
                My Orders
              </Link>
            </div>
          )}
        </div>

        {/* Desktop Right Section */}
        <div className="flex items-center space-x-3">
          {!isAuthenticated ? (
            !isAuthPage && (
              <Link to="/auth">
                <Button size="sm">Get Started</Button>
              </Link>
            )
          ) : (
            <>
              {/* Role-specific nav */}
              {role === 'ADMIN' && (
                <Link to="/admin" className="hidden md:inline-flex">
                  <Button variant="ghost" size="sm">Admin Dashboard</Button>
                </Link>
              )}
              {role === 'STAFF' && (
                <Link to="/staff" className="hidden md:inline-flex">
                  <Button variant="ghost" size="sm">Staff Dashboard</Button>
                </Link>
              )}
              {role === 'CUSTOMER' && (
                <>
                  <Link to="/dashboard" className="hidden md:inline-flex">
                    <Button variant="ghost" size="sm">
                      <LayoutDashboard className="h-4 w-4 mr-1.5" />
                      Dashboard
                    </Button>
                  </Link>
                  <Link to="/profile" className="hidden md:inline-flex">
                    <Button variant="ghost" size="sm">
                      <UserCircle className="h-4 w-4 mr-1.5" />
                      Profile
                    </Button>
                  </Link>
                </>
              )}

              {/* Cart Button (CUSTOMER only) */}
              {role === 'CUSTOMER' && (
                <Button variant="outline" size="icon" onClick={openCart} className="relative">
                  <ShoppingCart className="h-5 w-5" />
                  {cartItemCount > 0 && (
                    <span className="absolute -top-1.5 -right-1.5 bg-primary text-primary-foreground text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                      {cartItemCount}
                    </span>
                  )}
                </Button>
              )}

              {/* User info + Logout */}
              <div className="hidden md:flex items-center gap-3 border-l pl-4 ml-2">
                <div className="flex flex-col items-end">
                  <span className="text-sm font-medium leading-none">{user?.firstName} {user?.lastName}</span>
                  <span className="text-xs text-muted-foreground">{getRoleDisplayName(role)}</span>
                </div>
                <Button variant="ghost" size="icon" onClick={handleLogout} title="Logout" className="text-slate-500 hover:text-destructive">
                  <LogOut className="h-5 w-5" />
                </Button>
              </div>

              {/* Mobile menu button */}
              <Button
                variant="ghost"
                size="icon"
                className="md:hidden"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              >
                {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && isAuthenticated && (
        <div className="md:hidden border-t bg-background">
          <div className="container py-4 space-y-1">
            {role === 'CUSTOMER' && (
              <>
                <Link
                  to="/dashboard"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
                >
                  <LayoutDashboard className="h-4 w-4" /> Dashboard
                </Link>
                <Link
                  to="/products"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
                >
                  <ShoppingBag className="h-4 w-4" /> Products
                </Link>
                <Link
                  to="/orders"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
                >
                  <Package className="h-4 w-4" /> Orders
                </Link>
                <Link
                  to="/profile"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
                >
                  <UserCircle className="h-4 w-4" /> Profile
                </Link>
              </>
            )}
            {role === 'ADMIN' && (
              <Link
                to="/admin"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
              >
                <LayoutDashboard className="h-4 w-4" /> Admin Dashboard
              </Link>
            )}
            {role === 'STAFF' && (
              <Link
                to="/staff"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium hover:bg-slate-100 transition-colors"
              >
                <LayoutDashboard className="h-4 w-4" /> Staff Dashboard
              </Link>
            )}
            <div className="border-t pt-2 mt-2">
              <div className="px-3 py-2 text-sm text-muted-foreground">
                {user?.firstName} {user?.lastName} · {getRoleDisplayName(role)}
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium text-destructive hover:bg-destructive/10 w-full transition-colors"
              >
                <LogOut className="h-4 w-4" /> Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
