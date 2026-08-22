import { useState, useEffect } from 'react';
import { staffApi } from '../../api/staffApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { AlertTriangle, Package } from 'lucide-react';
import { Button } from '../../components/ui/Button';

export default function StaffInventory() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLowStock();
  }, []);

  const fetchLowStock = async () => {
    try {
      setLoading(true);
      const res = await staffApi.getLowStockProducts();
      setProducts(res.data || []);
    } catch (err) {
      setError('Failed to load low stock inventory');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Inventory Status</h1>
        <p className="text-slate-500 mt-2">View low stock and out of stock items</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            Low Stock Alerts
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : products.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <Package className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">Inventory is healthy</p>
              <p>No products are currently low on stock.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Product</th>
                    <th className="px-6 py-3">Category</th>
                    <th className="px-6 py-3">Stock Remaining</th>
                    <th className="px-6 py-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.id} className="bg-white border-b hover:bg-slate-50">
                      <td className="px-6 py-4 font-medium text-slate-900">
                        {product.name}
                        <div className="text-xs text-slate-500 mt-1">SKU: {product.sku}</div>
                      </td>
                      <td className="px-6 py-4">{product.categoryName || 'General'}</td>
                      <td className="px-6 py-4 font-bold">{product.availableQuantity || 0}</td>
                      <td className="px-6 py-4">
                        <Badge variant={(product.availableQuantity || 0) === 0 ? 'destructive' : 'warning'}>
                          {(product.availableQuantity || 0) === 0 ? 'Out of Stock' : 'Low Stock'}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
