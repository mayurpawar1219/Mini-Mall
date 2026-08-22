import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link, useParams, Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getDefaultRouteForRole } from '../../lib/authUtils';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../../components/ui/Card';
import { motion } from 'framer-motion';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { roleType } = useParams();
  const [serverError, setServerError] = useState('');

  // Validate roleType
  const validRoles = ['admin', 'staff', 'user'];
  if (!validRoles.includes(roleType)) {
    return <Navigate to="/auth" replace />;
  }

  const roleDisplayName = roleType.charAt(0).toUpperCase() + roleType.slice(1);
  const expectedRole = roleType === 'user' ? 'CUSTOMER' : roleType.toUpperCase();
  
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    try {
      setServerError('');
      const user = await login(data.email, data.password, expectedRole);
      
      // Redirect based on role
      const route = getDefaultRouteForRole(user.role);
      navigate(route);
      
    } catch (error) {
      setServerError(error);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-md"
    >
      <Card>
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold">{roleDisplayName} Login</CardTitle>
          <CardDescription>Enter your email to sign in to your {roleType} account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium leading-none" htmlFor="email">Email</label>
              <Input
                id="email"
                type="email"
                placeholder="m@example.com"
                error={errors.email?.message}
                {...register('email')}
              />
            </div>
            
            <div className="space-y-2">
              <label className="text-sm font-medium leading-none" htmlFor="password">Password</label>
              <Input
                id="password"
                type="password"
                error={errors.password?.message}
                {...register('password')}
              />
            </div>
            
            {serverError && (
              <div className="p-3 text-sm rounded-md bg-destructive/15 text-destructive font-medium">
                {serverError === 'Network Error' || serverError.includes('ERR_CONNECTION_REFUSED') 
                  ? 'Unable to connect to the server. Please try again.' 
                  : serverError}
              </div>
            )}
            
            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              Sign In
            </Button>
            
            <p className="text-xs text-center text-muted-foreground mt-4">
              Your workspace is selected automatically based on your account.
            </p>
          </form>
        </CardContent>
        <CardFooter className="flex justify-center">
          <p className="text-sm text-muted-foreground">
            Don't have an account?{' '}
            <Link to={`/auth/register/${roleType}`} className="text-primary hover:underline font-medium">
              Sign up
            </Link>
          </p>
        </CardFooter>
      </Card>
    </motion.div>
  );
}
