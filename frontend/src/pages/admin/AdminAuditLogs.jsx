import { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Activity } from 'lucide-react';
import { format } from 'date-fns';

export default function AdminAuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getAuditLogs({ page: 0, size: 100 });
      setLogs(res.data.content || []);
    } catch (err) {
      setError('Failed to load audit logs');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Audit Logs</h1>
        <p className="text-slate-500 mt-2">View recent administrative and system actions</p>
      </div>

      <Card>
        <CardHeader className="pb-3 border-b">
          <CardTitle>System Activity</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {error && <div className="p-6 text-red-500">{error}</div>}
          
          {loading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : logs.length === 0 ? (
            <div className="p-12 text-center text-slate-500 flex flex-col items-center">
              <Activity className="h-12 w-12 text-slate-300 mb-4" />
              <p className="text-lg font-medium text-slate-900">No activity recorded</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-700 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-6 py-3">Timestamp</th>
                    <th className="px-6 py-3">Actor</th>
                    <th className="px-6 py-3">Action</th>
                    <th className="px-6 py-3">Entity Type</th>
                    <th className="px-6 py-3">Entity ID</th>
                    <th className="px-6 py-3">Details</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id} className="bg-white border-b hover:bg-slate-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        {format(new Date(log.timestamp), 'MMM dd, HH:mm:ss')}
                      </td>
                      <td className="px-6 py-4">{log.actorId ? 'User' : 'System'}</td>
                      <td className="px-6 py-4 font-medium text-slate-900">{log.action}</td>
                      <td className="px-6 py-4">{log.entityType}</td>
                      <td className="px-6 py-4">{log.entityId}</td>
                      <td className="px-6 py-4 text-slate-500 max-w-xs truncate" title={log.details}>
                        {log.details}
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
