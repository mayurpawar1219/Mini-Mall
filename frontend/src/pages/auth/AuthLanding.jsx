import { Link } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { motion } from 'framer-motion';
import { ShieldAlert, Users, UserCircle } from 'lucide-react';
import { Button } from '../../components/ui/Button';

export default function AuthLanding() {
  const options = [
    {
      title: 'User',
      description: 'Login or create a customer account',
      icon: UserCircle,
      path: 'user',
      color: 'text-blue-500',
      bgColor: 'bg-blue-50'
    },
    {
      title: 'Staff',
      description: 'Access the staff operations dashboard',
      icon: Users,
      path: 'staff',
      color: 'text-emerald-500',
      bgColor: 'bg-emerald-50'
    },
    {
      title: 'Admin',
      description: 'Access the administrator control panel',
      icon: ShieldAlert,
      path: 'admin',
      color: 'text-purple-500',
      bgColor: 'bg-purple-50'
    }
  ];

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-3xl"
    >
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 mb-2">Select Workspace</h1>
        <p className="text-slate-500">Choose your account type to continue</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {options.map((option) => (
          <Card key={option.title} className="hover:border-primary transition-colors duration-200">
            <CardHeader className="text-center pb-2">
              <div className={`mx-auto p-4 rounded-full ${option.bgColor} mb-4`}>
                <option.icon className={`w-8 h-8 ${option.color}`} />
              </div>
              <CardTitle>{option.title}</CardTitle>
              <CardDescription className="h-10">{option.description}</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3 pt-4">
              <Link to={`/auth/login/${option.path}`}>
                <Button className="w-full">
                  {option.title} Login
                </Button>
              </Link>
              <Link to={`/auth/register/${option.path}`}>
                <Button variant="outline" className="w-full">
                  {option.title} Register
                </Button>
              </Link>
            </CardContent>
          </Card>
        ))}
      </div>
    </motion.div>
  );
}
