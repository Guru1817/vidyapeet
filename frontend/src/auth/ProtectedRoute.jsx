import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Spinner from '../components/Spinner';

/**
 * Guards a route by authentication and (optionally) allowed roles. Redirects to
 * login when unauthenticated, or to the user's home when the role is wrong.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, ready } = useAuth();

  if (!ready) {
    return <Spinner full label="Loading..." />;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homeFor(user.role)} replace />;
  }
  return children;
}

export function homeFor(role) {
  switch (role) {
    case 'SUPER_ADMIN':
      return '/superadmin/institutes';
    case 'INSTITUTE_ADMIN':
      return '/admin/dashboard';
    case 'STUDENT':
      return '/student/tests';
    default:
      return '/login';
  }
}
