import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { ArrowLeft, PackageSearch, Save } from 'lucide-react';

export default function AddProduct() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [categoriesError, setCategoriesError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  
  const [formData, setFormData] = useState({
    name: '',
    sku: '',
    categoryId: '',
    price: '',
    stockQuantity: '',
    description: '',
    imageUrl: ''
  });

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setCategoriesLoading(true);
        setCategoriesError(null);
        const res = await productApi.getCategories();
        setCategories(res.data);
      } catch (err) {
        console.error('Failed to load categories', err);
        setCategoriesError('Failed to load categories');
      } finally {
        setCategoriesLoading(false);
      }
    };
    fetchCategories();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const validate = () => {
    if (!formData.name.trim()) return 'Product Name is required';
    if (!formData.sku.trim()) return 'SKU is required';
    if (!formData.categoryId) return 'Category is required';
    
    const price = parseFloat(formData.price);
    if (isNaN(price) || price <= 0) return 'Price must be greater than 0';
    
    const stock = parseInt(formData.stockQuantity, 10);
    if (isNaN(stock) || stock < 0) return 'Stock Quantity cannot be negative';
    
    if (!formData.description.trim()) return 'Description is required';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      setLoading(true);
      await productApi.create({
        name: formData.name,
        sku: formData.sku,
        categoryId: parseInt(formData.categoryId, 10),
        price: parseFloat(formData.price),
        stockQuantity: parseInt(formData.stockQuantity, 10),
        description: formData.description,
        imageUrl: formData.imageUrl
      });
      
      setSuccess(true);
      setTimeout(() => {
        navigate('/admin/products');
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to add product. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/admin/products">
          <Button variant="outline" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">Add New Product</h1>
          <p className="text-slate-500 mt-1">Create a new product in the catalog</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-xl">
            <PackageSearch className="h-5 w-5 text-primary" />
            Product Details
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && <div className="p-3 bg-red-50 text-red-600 rounded-md text-sm font-medium">{error}</div>}
            {success && <div className="p-3 bg-green-50 text-green-700 rounded-md text-sm font-medium">Product added successfully! Redirecting...</div>}
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label htmlFor="name" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Product Name <span className="text-red-500">*</span></label>
                <Input id="name" name="name" value={formData.name} onChange={handleChange} placeholder="e.g. Amul Taaza Milk" required />
              </div>
              
              <div className="space-y-2">
                <label htmlFor="sku" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">SKU (Barcode/Identifier) <span className="text-red-500">*</span></label>
                <Input id="sku" name="sku" value={formData.sku} onChange={handleChange} placeholder="e.g. MILK-AMUL-1L" required />
              </div>

              <div className="space-y-2">
                <label htmlFor="categoryId" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Category <span className="text-red-500">*</span></label>
                <select 
                  id="categoryId" 
                  name="categoryId" 
                  value={formData.categoryId} 
                  onChange={handleChange}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  required
                  disabled={categoriesLoading || !!categoriesError || categories.length === 0}
                >
                  <option value="" disabled>
                    {categoriesLoading ? 'Loading categories...' : 
                     categoriesError ? 'Error loading categories' : 
                     categories.length === 0 ? 'No categories available' : 
                     'Select a category'}
                  </option>
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label htmlFor="price" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Price (₹) <span className="text-red-500">*</span></label>
                <Input id="price" name="price" type="number" step="0.01" min="0.01" value={formData.price} onChange={handleChange} placeholder="0.00" required />
              </div>

              <div className="space-y-2">
                <label htmlFor="stockQuantity" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Initial Stock Quantity <span className="text-red-500">*</span></label>
                <Input id="stockQuantity" name="stockQuantity" type="number" min="0" step="1" value={formData.stockQuantity} onChange={handleChange} placeholder="0" required />
              </div>

              <div className="space-y-2">
                <label htmlFor="imageUrl" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Product Image URL</label>
                <Input id="imageUrl" name="imageUrl" type="url" value={formData.imageUrl} onChange={handleChange} placeholder="https://example.com/image.jpg" />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="description" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Description <span className="text-red-500">*</span></label>
              <textarea 
                id="description" 
                name="description" 
                value={formData.description} 
                onChange={handleChange}
                rows="4" 
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Product description and details..."
                required
              />
            </div>

            <div className="pt-4 flex justify-end">
              <Button type="submit" disabled={loading} className="w-full sm:w-auto">
                {loading ? 'Saving...' : (
                  <>
                    <Save className="mr-2 h-4 w-4" /> Save Product
                  </>
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
