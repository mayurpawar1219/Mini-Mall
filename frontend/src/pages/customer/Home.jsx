import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, ShoppingBag } from 'lucide-react';
import { productApi } from '../../api/productApi';
import ProductCard from '../../components/product/ProductCard';
import { Skeleton } from '../../components/ui/Skeleton';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';

export default function Home() {
  const { isAuthenticated, role } = useAuth();
  const { addToCart } = useCart();

  const { data: productsData, isLoading } = useQuery({
    queryKey: ['products', { size: 8 }],
    queryFn: () => productApi.getAll({ size: 8 }),
  });

  const { data: categoriesData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories(),
  });

  const handleAddToCart = (product) => {
    if (!isAuthenticated || role !== 'CUSTOMER') {
      return; // Let the UI handle showing login prompt
    }
    addToCart(product.id, 1);
  };

  const categories = categoriesData?.data || [];

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50 py-16 md:py-24">
        <div className="container px-4 md:px-6">
          <div className="grid gap-6 lg:grid-cols-[1fr_400px] lg:gap-12 xl:grid-cols-[1fr_600px]">
            <div className="flex flex-col justify-center space-y-4">
              <div className="space-y-2">
                <h1 className="text-3xl font-bold tracking-tighter sm:text-5xl xl:text-6xl/none text-emerald-950">
                  Fresh Groceries, <br />
                  Delivered Daily.
                </h1>
                <p className="max-w-[600px] text-emerald-800/80 md:text-xl">
                  Shop fresh produce, pantry staples, and household essentials. Premium quality guaranteed with Mini D-Mart.
                </p>
              </div>
              <div className="flex flex-col gap-2 min-[400px]:flex-row">
                <Link to="/products">
                  <Button size="lg" className="w-full min-[400px]:w-auto font-medium">
                    <ShoppingBag className="mr-2 h-5 w-5" /> Shop Now
                  </Button>
                </Link>
              </div>
            </div>
            <div className="hidden lg:flex items-center justify-center">
              <div className="w-full h-[400px] bg-emerald-200/50 rounded-full flex items-center justify-center text-emerald-700">
                <ShoppingBag className="w-32 h-32 opacity-50" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories Section */}
      {categories.length > 0 && (
        <section className="py-12 bg-white border-b">
          <div className="container px-4 md:px-6">
            <h2 className="text-2xl font-bold tracking-tight mb-6">Shop by Category</h2>
            <div className="flex gap-3 overflow-x-auto pb-2 scrollbar-thin">
              {categories.slice(0, 8).map(cat => (
                <Link
                  key={cat.id}
                  to={`/products?category=${cat.id}`}
                  className="flex-shrink-0 px-5 py-3 bg-slate-50 hover:bg-emerald-50 border rounded-full text-sm font-medium transition-colors hover:border-emerald-200 hover:text-emerald-700"
                >
                  {cat.name}
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Featured Products */}
      <section className="py-16 bg-white">
        <div className="container px-4 md:px-6">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl font-bold tracking-tight">Featured Products</h2>
            <Link to="/products" className="flex items-center text-sm font-medium text-primary hover:underline">
              View all <ArrowRight className="ml-1 h-4 w-4" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {isLoading
              ? Array.from({ length: 8 }).map((_, i) => (
                  <div key={i} className="space-y-4">
                    <Skeleton className="h-[250px] w-full rounded-xl" />
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-4 w-1/2" />
                  </div>
                ))
              : productsData?.data?.content?.map((product) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    onAddToCart={handleAddToCart}
                  />
                ))}
          </div>

          {!isLoading && (!productsData?.data?.content || productsData.data.content.length === 0) && (
            <div className="text-center py-20 bg-slate-50 rounded-xl border border-dashed">
              <ShoppingBag className="h-12 w-12 text-slate-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium">No products available yet</h3>
              <p className="text-sm text-muted-foreground mt-1">Check back soon for fresh groceries!</p>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
