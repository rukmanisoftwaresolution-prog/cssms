import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from '../components/LoadingOverlay';
import ForbiddenPage from '../pages/errors/ForbiddenPage';

/** Requires an authenticated user; otherwise redirects to /login */
export function ProtectedRoute({ children }) {
  const { isAuthenticated, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <PageLoader minHeight="100vh" />;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  return children;
}

/** Shows the 403 page unless the role has `view` (or the given action) on `resource` */
export function RoleRoute({ resource, action = 'view', children }) {
  const { can } = useAuth();
  if (!can(resource, action)) return <ForbiddenPage />;
  return children;
}

/** Login/reset pages: bounce logged-in users to the dashboard */
export function PublicOnlyRoute({ children }) {
  const { isAuthenticated, initializing } = useAuth();
  if (initializing) return <PageLoader minHeight="100vh" />;
  if (isAuthenticated) return <Navigate to="/" replace />;
  return children;
}
