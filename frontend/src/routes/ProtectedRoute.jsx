import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/common/Spinner';
import ForbiddenPage from '../pages/errors/ForbiddenPage';

/** Redirige vers la connexion si besoin. Le contrôle réel des droits reste côté backend. */
export function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Spinner fullscreen />;
  if (!user) return <Navigate to="/connexion" replace state={{ from: location.pathname }} />;
  if (user.mustChangePassword && location.pathname !== '/profil') return <Navigate to="/profil" replace />;
  return <Outlet />;
}

export function RequirePermission({ permission, children }) {
  const { can } = useAuth();
  return can(permission) ? children : <ForbiddenPage />;
}
