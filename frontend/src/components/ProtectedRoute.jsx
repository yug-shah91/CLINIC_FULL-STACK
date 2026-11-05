import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Guards a group of routes.
//   <ProtectedRoute />            -> must be logged in
//   <ProtectedRoute role="ADMIN"/> -> must be logged in AND have that role
// <Outlet /> is where the child route renders when the check passes.
export default function ProtectedRoute({ role }) {
  const { user, checking } = useAuth();
  const location = useLocation();

  if (checking) return null; // wait for GET /auth/me before deciding
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}
