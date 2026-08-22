import { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { AlertCircle, Plus, Calendar as CalendarIcon, Clock } from 'lucide-react';
import toast from 'react-hot-toast';
import { format, parseISO } from 'date-fns';

export default function AdminPickupSlots() {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Create Form State
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [startTime, setStartTime] = useState('10:00');
  const [capacity, setCapacity] = useState(10);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchSlots();
  }, [date]); // Re-fetch when date changes

  const fetchSlots = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await adminApi.getPickupSlots(date);
      if (res.success) {
        setSlots(res.data);
      } else {
        setError(res.message);
      }
    } catch (err) {
      console.error(err);
      setError('Failed to load pickup slots');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSlot = async (e) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      
      // Calculate end time (always +1 hour)
      const startDateTime = new Date(`${date}T${startTime}:00`);
      const endDateTime = new Date(startDateTime.getTime() + 60 * 60 * 1000);
      
      const payload = {
        date: date,
        startTime: startDateTime.toISOString(),
        endTime: endDateTime.toISOString(),
        capacity: parseInt(capacity),
        enabled: true
      };

      const res = await adminApi.createPickupSlot(payload);
      if (res.success) {
        toast.success('Pickup slot created successfully');
        fetchSlots();
      } else {
        toast.error(res.message || 'Failed to create slot');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create slot');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (slot) => {
    try {
      const payload = {
        date: slot.date,
        startTime: slot.startTime,
        endTime: slot.endTime,
        capacity: slot.capacity,
        enabled: !slot.enabled
      };
      
      const res = await adminApi.updatePickupSlot(slot.id, payload);
      if (res.success) {
        toast.success(`Slot ${slot.enabled ? 'disabled' : 'enabled'} successfully`);
        fetchSlots();
      }
    } catch (err) {
      toast.error('Failed to update slot status');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-slate-800">Manage Pickup Slots</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Create Slot Form */}
        <Card className="md:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Plus className="h-5 w-5 text-primary" />
              Create New Slot
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateSlot} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Date</label>
                <input 
                  type="date" 
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Start Time (1 Hour Duration)</label>
                <input 
                  type="time" 
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  step="3600"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Capacity</label>
                <input 
                  type="number" 
                  value={capacity}
                  onChange={(e) => setCapacity(e.target.value)}
                  min="1"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>

              <Button type="submit" disabled={submitting} className="w-full">
                {submitting ? 'Creating...' : 'Create Slot'}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Slot List */}
        <Card className="md:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xl">Slots for {format(parseISO(date), 'MMMM d, yyyy')}</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center p-8"><span className="loader"></span></div>
            ) : error ? (
              <div className="flex items-center gap-2 text-red-600 bg-red-50 p-4 rounded-md">
                <AlertCircle className="h-5 w-5" />
                <p>{error}</p>
              </div>
            ) : slots.length === 0 ? (
              <div className="text-center p-8 text-slate-500 bg-slate-50 rounded-lg border border-dashed border-slate-300">
                <CalendarIcon className="mx-auto h-12 w-12 text-slate-400 mb-2" />
                <p>No pickup slots created for this date.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead className="text-xs text-slate-500 uppercase bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 rounded-tl-lg">Time</th>
                      <th className="px-4 py-3">Bookings</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3 rounded-tr-lg">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {slots.map((slot) => {
                      const isFull = slot.currentBookings >= slot.capacity;
                      return (
                        <tr key={slot.id} className="border-b hover:bg-slate-50">
                          <td className="px-4 py-4 font-medium text-slate-900">
                            <div className="flex items-center gap-2">
                              <Clock className="h-4 w-4 text-slate-400" />
                              {format(parseISO(slot.startTime), 'h:mm a')} - {format(parseISO(slot.endTime), 'h:mm a')}
                            </div>
                          </td>
                          <td className="px-4 py-4">
                            <span className={`font-semibold ${isFull ? 'text-red-600' : 'text-slate-700'}`}>
                              {slot.currentBookings}
                            </span>
                            <span className="text-slate-500"> / {slot.capacity}</span>
                          </td>
                          <td className="px-4 py-4">
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                              slot.enabled 
                                ? isFull ? 'bg-orange-100 text-orange-700' : 'bg-green-100 text-green-700'
                                : 'bg-slate-100 text-slate-700'
                            }`}>
                              {!slot.enabled ? 'Disabled' : isFull ? 'Full' : 'Active'}
                            </span>
                          </td>
                          <td className="px-4 py-4">
                            <Button 
                              variant="outline" 
                              size="sm"
                              onClick={() => handleToggleStatus(slot)}
                            >
                              {slot.enabled ? 'Disable' : 'Enable'}
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
