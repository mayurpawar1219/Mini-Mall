import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link, useParams, Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../../components/ui/Card';
import { motion } from 'framer-motion';

const registerSchema = z.object({
  firstName: z.string().min(2, 'First name must be at least 2 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters'),
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  phone: z.string().optional(),
  setupToken: z.string().optional(),
});

export default function Register() {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const { roleType } = useParams();
  const [serverError, setServerError] = useState('');

  // Validate roleType
  const validRoles = ['admin', 'staff', 'user'];
  if (!validRoles.includes(roleType)) {
    return <Navigate to="/auth" replace />;
  }

  const roleDisplayName = roleType.charAt(0).toUpperCase() + roleType.slice(1);
  const requiresSetupToken = roleType === 'admin' || roleType === 'staff';
  
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data) => {
    try {
      setServerError('');
      await registerUser(data, roleType);
      navigate('/');
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
          <CardTitle className="text-2xl font-bold">{roleDisplayName} Registration</CardTitle>
          <CardDescription>Enter your information to create a {roleType} account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium leading-none" htmlFor="firstName">First Name</label>
                <Input
                  id="firstName"
                  error={errors.firstName?.message}
                  {...register('firstName')}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium leading-none" htmlFor="lastName">Last Name</label>
                <Input
                  id="lastName"
                  error={errors.lastName?.message}
                  {...register('lastName')}
                />
              </div>
            </div>

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
              <label className="text-sm font-medium leading-none" htmlFor="phone">Phone (optional)</label>
              <Input
                id="phone"
                type="tel"
                error={errors.phone?.message}
                {...register('phone')}
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
            
            {requiresSetupToken && (
              <div className="space-y-2 p-4 border rounded-md bg-slate-50 mt-4">
                <label className="text-sm font-medium text-slate-800 leading-none flex items-center gap-2" htmlFor="setupToken">
                  Setup Token <span className="text-xs text-muted-foreground font-normal">(Required for {roleDisplayName})</span>
                </label>
                <Input
                  id="setupToken"
                  type="password"
                  placeholder="Enter secret token"
                  error={errors.setupToken?.message}
                  {...register('setupToken')}
                />
              </div>
            )}
            
            {serverError && (
              <div className="p-3 text-sm rounded-md bg-destructive/15 text-destructive font-medium">
                {serverError === 'Network Error' || serverError.includes('ERR_CONNECTION_REFUSED') 
                  ? 'Unable to connect to the server. Please try again.' 
                  : serverError}
              </div>
            )}
            
            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              Create Account
            </Button>
          </form>
        </CardContent>
        <CardFooter className="flex justify-center">
          <p className="text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to={`/auth/login/${roleType}`} className="text-primary hover:underline font-medium">
              Sign in
            </Link>
          </p>
        </CardFooter>
      </Card>
    </motion.div>
  );
}
