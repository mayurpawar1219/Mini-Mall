import { Link } from 'react-router-dom';
import { ShoppingCart } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';

export default function ProductCard({ product, onAddToCart }) {
  const isOutOfStock = product.stockQuantity === 0;

  return (
    <Card className="overflow-hidden group h-full flex flex-col hover:border-primary/50 transition-colors">
      <div className="relative aspect-square bg-slate-100 p-6 flex items-center justify-center overflow-hidden">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover absolute inset-0 group-hover:scale-105 transition-transform duration-300"
            onError={(e) => {
              e.target.style.display = 'none';
              e.target.nextSibling.style.display = 'flex';
            }}
          />
        ) : null}
        <div
          className={`w-32 h-32 bg-slate-200 rounded-full items-center justify-center text-slate-400 group-hover:scale-105 transition-transform duration-300 ${product.imageUrl ? 'hidden' : 'flex'}`}
        >
          <span className="text-4xl font-bold">{product.name.charAt(0)}</span>
        </div>

        {isOutOfStock && (
          <div className="absolute top-2 right-2 z-10">
            <Badge variant="destructive">Out of Stock</Badge>
          </div>
        )}
      </div>

      <CardContent className="p-4 flex flex-col flex-1">
        <div className="text-xs text-muted-foreground mb-1 font-medium tracking-wide uppercase">
          {product.category?.name || 'Groceries'}
        </div>
        <Link to={`/products/${product.id}`} className="font-semibold text-lg hover:text-primary transition-colors line-clamp-2 mb-2">
          {product.name}
        </Link>

        <div className="mt-auto pt-4 flex items-center justify-between">
          <div className="font-bold text-lg">${product.price.toFixed(2)}</div>
          <Button
            size="sm"
            variant={isOutOfStock ? "secondary" : "default"}
            disabled={isOutOfStock}
            onClick={() => onAddToCart && onAddToCart(product)}
            className="rounded-full px-4"
          >
            {isOutOfStock ? 'Sold Out' : <><ShoppingCart className="h-4 w-4 mr-2" /> Add</>}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
