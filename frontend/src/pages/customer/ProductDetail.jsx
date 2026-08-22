import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { productApi } from '../../api/productApi';
import { useCart } from '../../context/CartContext';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { ArrowLeft, ShoppingCart, Minus, Plus } from 'lucide-react';
import { useState } from 'react';

export default function ProductDetail() {
  const { id } = useParams();
  const { addToCart } = useCart();
  const [quantity, setQuantity] = useState(1);
  
  const { data, isLoading, error } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productApi.getById(id),
  });

  const product = data?.data;
  const isOutOfStock = product?.stockQuantity === 0;

  if (isLoading) {
    return (
      <div className="container py-8 px-4 md:px-6">
        <div className="mb-6"><Skeleton className="h-6 w-32" /></div>
        <div className="grid md:grid-cols-2 gap-10">
          <Skeleton className="aspect-square rounded-2xl w-full" />
          <div className="space-y-6 pt-4">
            <Skeleton className="h-10 w-3/4" />
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-8 w-32" />
            <div className="space-y-2 pt-6">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-2/3" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="container py-20 px-4 text-center">
        <h2 className="text-2xl font-bold mb-2">Product Not Found</h2>
        <p className="text-muted-foreground mb-6">The product you are looking for doesn't exist or has been removed.</p>
        <Link to="/products">
          <Button>Back to Products</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="container py-8 px-4 md:px-6">
      <Link to="/products" className="inline-flex items-center text-sm font-medium text-muted-foreground hover:text-primary mb-8 transition-colors">
        <ArrowLeft className="mr-2 h-4 w-4" /> Back to all products
      </Link>
      
      <div className="grid md:grid-cols-2 gap-10 lg:gap-16">
        {/* Product Image Area */}
        <div className="bg-slate-100 rounded-3xl p-10 flex items-center justify-center aspect-square relative border border-slate-200 shadow-sm overflow-hidden">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover absolute inset-0 rounded-3xl"
              onError={(e) => {
                e.target.style.display = 'none';
                e.target.nextSibling.style.display = 'flex';
              }}
            />
          ) : null}
          <div className={`w-48 h-48 bg-white rounded-full items-center justify-center shadow-lg ${product.imageUrl ? 'hidden' : 'flex'}`}>
             <span className="text-7xl font-bold text-slate-300">{product.name.charAt(0)}</span>
          </div>
          {isOutOfStock && (
            <div className="absolute top-6 right-6 z-10">
              <Badge variant="destructive" className="text-sm px-4 py-1">Out of Stock</Badge>
            </div>
          )}
        </div>
        
        {/* Product Info Area */}
        <div className="flex flex-col pt-4 md:pt-10">
          <div className="mb-2">
            <Badge variant="secondary" className="mb-2 uppercase tracking-wider">{product.categoryName || product.category?.name || 'Category'}</Badge>
          </div>
          
          <h1 className="text-3xl md:text-4xl font-bold tracking-tight text-slate-900 mb-4">
            {product.name}
          </h1>
          
          <div className="text-3xl font-bold text-primary mb-6">
            ${product.price.toFixed(2)}
          </div>
          
          <div className="prose prose-sm md:prose-base text-slate-600 mb-8 max-w-none">
            <p>{product.description || "Fresh from the farm to your table. Mini D-Mart guarantees the highest quality for all our products."}</p>
          </div>
          
          <div className="mt-auto border-t pt-8">
            <div className="flex items-center gap-6 mb-6">
              <div className="flex items-center justify-between border rounded-lg p-1 w-32 bg-white">
                <button 
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || isOutOfStock}
                  className="w-10 h-10 flex items-center justify-center hover:bg-slate-100 rounded-md transition-colors disabled:opacity-50"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="font-semibold text-lg w-8 text-center">{quantity}</span>
                <button 
                  onClick={() => setQuantity(Math.min(product.stockQuantity, quantity + 1))}
                  disabled={quantity >= product.stockQuantity || isOutOfStock}
                  className="w-10 h-10 flex items-center justify-center hover:bg-slate-100 rounded-md transition-colors disabled:opacity-50"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <div className="text-sm text-muted-foreground">
                {product.stockQuantity > 0 ? (
                  <span className="text-emerald-600 font-medium">{product.stockQuantity} in stock</span>
                ) : (
                  <span className="text-destructive font-medium">Currently unavailable</span>
                )}
              </div>
            </div>
            
            <Button 
              size="lg" 
              className="w-full h-14 text-lg font-semibold rounded-xl"
              disabled={isOutOfStock}
              onClick={() => addToCart(product.id, quantity)}
            >
              <ShoppingCart className="mr-2 h-5 w-5" />
              {isOutOfStock ? 'Out of Stock' : 'Add to Cart'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
