import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

type ProtectedRouteProps = {
  permission?: string;
  /** Require at least one of these permissions (ignored if `permission` is set). */
  anyOf?: string[];
  /** If set, user must hold at least one of these roles. */
  roles?: string[];
  /** If set, users holding any of these roles are denied (UX isolation; backend still authoritative). */
  denyRoles?: string[];
};

export function ProtectedRoute({ permission, anyOf, roles, denyRoles }: ProtectedRouteProps) {
  const { isAuthenticated, loading, hasPermission, hasAnyPermission, hasRole } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-textSecondary" role="status">
        Checking session…
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  const roleDenied =
    (roles && roles.length > 0 && !roles.some((role) => hasRole(role))) ||
    (denyRoles && denyRoles.length > 0 && denyRoles.some((role) => hasRole(role)));

  const denied =
    roleDenied ||
    (permission && !hasPermission(permission)) ||
    (!permission && anyOf && anyOf.length > 0 && !hasAnyPermission(...anyOf));

  if (denied) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-2 p-8 text-center">
        <h1 className="text-xl font-semibold text-textPrimary">Access denied</h1>
        <p className="text-sm text-textSecondary">You do not have permission to view this page.</p>
      </div>
    );
  }

  return <Outlet />;
}
