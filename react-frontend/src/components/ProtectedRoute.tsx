import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

type Props = {
  children: ReactNode;
};

const ProtectedRoute = ({ children }: Props) => {
  const location = useLocation();
  const hasToken = Boolean(localStorage.getItem('token') || localStorage.getItem('adminToken'));

  if (!hasToken) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
