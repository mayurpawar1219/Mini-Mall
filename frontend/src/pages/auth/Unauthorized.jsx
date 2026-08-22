import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getDefaultRouteForRole } from '../../lib/authUtils';
import { Button } from '../../components/ui/Button';
import { ShieldAlert } from 'lucide-react';

export default function Unauthorized() {
  const navigate = useNavigate();
  const { role } = useAuth();

  const handleGoToDashboard = () => {
    navigate(getDefaultRouteForRole(role));
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
      <ShieldAlert className="w-20 h-20 text-destructive mb-6" />
      <h1 className="text-4xl font-bold tracking-tight text-slate-900 mb-2">
        403 Access Denied
      </h1>
      <p className="text-lg text-slate-600 mb-8 max-w-md">
        You don't have permission to access this area.
      </p>
      
      <Button onClick={handleGoToDashboard} size="lg">
        Go to Dashboard
      </Button>
    </div>
  );
}
