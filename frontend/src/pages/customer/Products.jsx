import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import ProductCard from '../../components/product/ProductCard';
import { Skeleton } from '../../components/ui/Skeleton';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { Search, SlidersHorizontal, X } from 'lucide-react';

export default function Products() {
  const [searchParams] = useSearchParams();
  const initialCategory = searchParams.get('category') ? Number(searchParams.get('category')) : null;

  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  const [showInStockOnly, setShowInStockOnly] = useState(false);

  const { addToCart } = useCart();
  const { isAuthenticated, role } = useAuth();

  // Debounce search
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    clearTimeout(window.__searchTimer);
    window.__searchTimer = setTimeout(() => {
      setDebouncedSearch(e.target.value);
    }, 400);
  };

  const { data: productsData, isLoading, error } = useQuery({
    queryKey: ['products', { search: debouncedSearch, categoryId: selectedCategory }],
    queryFn: () => productApi.getAll({
      search: debouncedSearch || undefined,
      categoryId: selectedCategory || undefined,
    }),
  });

  const { data: categoriesData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories(),
  });

  // The backend returns { success: true, data: [...] } — data is a flat array
  const rawProducts = productsData?.data || [];
  const products = useMemo(() => {
    if (showInStockOnly) {
      return rawProducts.filter(p => p.stockQuantity > 0);
    }
    return rawProducts;
  }, [rawProducts, showInStockOnly]);

  const categories = categoriesData?.data || [];

  const handleAddToCart = (product) => {
    if (!isAuthenticated || role !== 'CUSTOMER') return;
    addToCart(product.id, 1);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setDebouncedSearch('');
    setSelectedCategory(null);
    setShowInStockOnly(false);
  };

  const hasActiveFilters = debouncedSearch || selectedCategory || showInStockOnly;

  return (
    <div className="container py-8 px-4 md:px-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Our Products</h1>
          <p className="text-muted-foreground">Find fresh groceries and daily essentials.</p>
        </div>

        <div className="relative w-full md:w-[300px]">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search products..."
            className="pl-9"
            value={searchTerm}
            onChange={handleSearchChange}
          />
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar filters */}
        <div className="w-full md:w-64 space-y-6 flex-shrink-0">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold flex items-center gap-2">
                <SlidersHorizontal className="h-4 w-4" /> Filters
              </h3>
              {hasActiveFilters && (
                <button onClick={clearFilters} className="text-xs text-primary hover:underline flex items-center gap-1">
                  <X className="h-3 w-3" /> Clear
                </button>
              )}
            </div>
          </div>

          <div>
            <h3 className="font-semibold mb-3">Categories</h3>
            <div className="space-y-1">
              <button
                onClick={() => setSelectedCategory(null)}
                className={`block w-full text-left px-3 py-2 text-sm rounded-md transition-colors ${
                  selectedCategory === null
                    ? 'bg-primary/10 text-primary font-medium'
                    : 'text-muted-foreground hover:bg-slate-100 hover:text-foreground'
                }`}
              >
                All Products
              </button>
              {categories.map(category => (
                <button
                  key={category.id}
                  onClick={() => setSelectedCategory(category.id)}
                  className={`block w-full text-left px-3 py-2 text-sm rounded-md transition-colors ${
                    selectedCategory === category.id
                      ? 'bg-primary/10 text-primary font-medium'
                      : 'text-muted-foreground hover:bg-slate-100 hover:text-foreground'
                  }`}
                >
                  {category.name}
                </button>
              ))}
            </div>
          </div>

          <div>
            <h3 className="font-semibold mb-3">Availability</h3>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={showInStockOnly}
                onChange={(e) => setShowInStockOnly(e.target.checked)}
                className="rounded border-slate-300"
              />
              In stock only
            </label>
          </div>
        </div>

        {/* Product Grid */}
        <div className="flex-1">
          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {Array.from({ length: 9 }).map((_, i) => (
                <div key={i} className="space-y-4">
                  <Skeleton className="h-[250px] w-full rounded-xl" />
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center py-20 text-center border rounded-xl border-dashed bg-red-50">
              <div className="text-4xl mb-4">⚠️</div>
              <h3 className="text-lg font-semibold text-destructive">Failed to load products</h3>
              <p className="text-muted-foreground max-w-sm mt-1">
                {error.response?.data?.message || 'An unexpected error occurred. Please try again.'}
              </p>
            </div>
          ) : products.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-center border rounded-xl border-dashed bg-slate-50">
              <div className="text-4xl mb-4">🛒</div>
              <h3 className="text-lg font-semibold">No products found</h3>
              <p className="text-muted-foreground max-w-sm mt-1">
                Try adjusting your search or category filters to find what you're looking for.
              </p>
              {hasActiveFilters && (
                <Button variant="outline" className="mt-4" onClick={clearFilters}>
                  Clear Filters
                </Button>
              )}
            </div>
          ) : (
            <>
              <div className="text-sm text-muted-foreground mb-4">
                Showing {products.length} product{products.length !== 1 ? 's' : ''}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {products.map(product => (
                  <ProductCard
                    key={product.id}
                    product={{
                      ...product,
                      // Normalize for ProductCard: it expects product.category?.name
                      category: { id: product.categoryId, name: product.categoryName },
                    }}
                    onAddToCart={handleAddToCart}
                  />
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
