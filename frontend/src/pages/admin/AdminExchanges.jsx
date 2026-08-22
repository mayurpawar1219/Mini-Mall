import { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { ArrowLeftRight, Check, X } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { format } from 'date-fns';

export default function AdminExchanges() {
  const [exchanges, setExchanges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchExchanges();
  }, []);

  const fetchExchanges = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getExchanges({ page: 0, size: 50 });
      setExchanges(res.data.data?.content || []);
    } catch (err) {
      setError('Failed to load exchange requests');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id, status) => {
    try {
      await adminApi.updateExchangeStatus(id, status);
      fetchExchanges();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const getStatusBadge = (status) => {
    const variants = {
      PENDING: 'warning',
      APPROVED: 'success',
      REJECTED: 'destructive',
      COMPLETED: 'default',
    };
    return <Badge variant={variants[status] || 'secondary'}>{status}</Badge>;
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Exchange Requests</h1>
        <p className="text-slate-500 mt-2">Manage customer exchange requests</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle>All Exchanges</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : exchanges.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <ArrowLeftRight className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">No exchange requests found</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Order #</th>
                    <th className="px-6 py-3">Original Product</th>
                    <th className="px-6 py-3">Replacement</th>
                    <th className="px-6 py-3">Reason</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3">Date</th>
                    <th className="px-6 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {exchanges.map((req) => (
                    <tr key={req.id} className="bg-white border-b hover:bg-slate-50">
                      <td className="px-6 py-4 font-medium">{req.orderNumber}</td>
                      <td className="px-6 py-4">{req.originalProductName}</td>
                      <td className="px-6 py-4">{req.replacementProductName}</td>
                      <td className="px-6 py-4 text-slate-500">{req.reason}</td>
                      <td className="px-6 py-4">{getStatusBadge(req.status)}</td>
                      <td className="px-6 py-4">
                        {format(new Date(req.createdAt), 'MMM dd, yyyy HH:mm')}
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        {req.status === 'PENDING' && (
                          <>
                            <Button variant="outline" size="sm" className="text-green-600 border-green-200 hover:bg-green-50" onClick={() => handleUpdateStatus(req.id, 'APPROVED')}>
                              <Check className="h-4 w-4 mr-1" /> Approve
                            </Button>
                            <Button variant="outline" size="sm" className="text-red-600 border-red-200 hover:bg-red-50" onClick={() => handleUpdateStatus(req.id, 'REJECTED')}>
                              <X className="h-4 w-4 mr-1" /> Reject
                            </Button>
                          </>
                        )}
                        {req.status === 'APPROVED' && (
                          <Button variant="outline" size="sm" onClick={() => handleUpdateStatus(req.id, 'COMPLETED')}>
                            Complete
                          </Button>
                        )}
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
