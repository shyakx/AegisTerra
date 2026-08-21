import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Outlet, NavLink, useNavigate, Link } from 'react-router-dom';
import { Bell, LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { notificationsApi } from '../api/notifications';
import { buildNavigation, portalHeader } from '../navigation/buildNavigation';
import { PageTransition } from '../motion/PageTransition';
import { PoweredBy } from '../components/PoweredBy';

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout, hasPermission, hasRole, hasAnyPermission } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const header = portalHeader(user?.roles);

  const unreadQuery = useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: () => notificationsApi.unreadCount(),
    enabled: hasPermission('notifications:read'),
    refetchInterval: 30000
  });
  const unread = unreadQuery.data?.count ?? 0;

  const visibleGroups = useMemo(
    () => buildNavigation({ hasPermission, hasRole, hasAnyPermission, farmerId: user?.farmerId }),
    [hasPermission, hasRole, hasAnyPermission, user?.farmerId]
  );

  async function handleLogout() {
    await logout();
    navigate('/', { replace: true });
  }

  const nav = (
    <nav className="space-y-5 pr-1" aria-label="Primary">
      {visibleGroups.map((group) => (
        <div key={group.label}>
          <p className="mb-2 px-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-200/70">
            {group.label}
          </p>
          <div className="space-y-1">
            {group.items.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={`${group.label}-${item.to}`}
                  to={item.to}
                  end={item.to === '/app'}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-medium ${
                      isActive ? 'bg-white/15 text-white' : 'text-emerald-100 hover:bg-white/10'
                    }`
                  }
                >
                  <Icon className="h-4 w-4 shrink-0" aria-hidden />
                  <span className="truncate">{item.label}</span>
                </NavLink>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen bg-background text-textPrimary">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 flex-col overflow-hidden bg-sidebar p-5 text-white lg:flex">
        <div className="at-grid pointer-events-none absolute inset-0" aria-hidden />
        <div className="relative z-10 shrink-0 px-2">
          <Link to="/app" className="block rounded-2xl bg-white px-3 py-2">
            <img
              src="/aegisterra-logo.png"
              alt="AegisTerra"
              className="h-12 w-auto object-contain"
            />
          </Link>
          <h1 className="font-display mt-3 text-xl font-semibold leading-snug">Climate risk intelligence</h1>
          <p className="mt-2 text-xs text-emerald-100/70">Partner workspace</p>
        </div>
        <div className="relative z-10 mt-6 min-h-0 flex-1 overflow-y-auto overscroll-contain">
          {nav}
        </div>
        <div className="relative z-10 mt-4 shrink-0 border-t border-white/10 pt-4">
          <div className="rounded-2xl bg-white/10 px-4 py-3">
            <p className="text-sm font-medium">{user?.displayName || user?.username || 'Signed in'}</p>
            <p className="mt-0.5 text-xs text-emerald-100/80">{user?.roles?.[0] ?? 'Operator'}</p>
          </div>
          <PoweredBy className="mt-3 text-center text-emerald-100/80" />
        </div>
      </aside>

      {mobileOpen ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            className="absolute inset-0 bg-black/40"
            aria-label="Close menu"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-sidebar p-5 text-white">
            <div className="mb-6 flex shrink-0 items-center justify-between gap-3 px-2">
            <Link to="/app" className="rounded-xl bg-white px-2 py-1.5">
                <img
                  src="/aegisterra-logo.png"
                  alt="AegisTerra"
                  className="h-9 w-auto object-contain"
                />
              </Link>
              <button type="button" aria-label="Close navigation" onClick={() => setMobileOpen(false)}>
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">{nav}</div>
            <div className="mt-4 shrink-0 border-t border-white/10 pt-4">
              <PoweredBy className="text-center text-emerald-100/80" />
            </div>
          </aside>
        </div>
      ) : null}

      <div className="lg:pl-72">
        <header className="sticky top-0 z-20 bg-surface px-4 py-3 sm:px-6">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <button
                type="button"
                className="rounded-full border border-border p-2 text-textSecondary lg:hidden"
                aria-label="Open navigation"
                onClick={() => setMobileOpen(true)}
              >
                <Menu className="h-5 w-5" />
              </button>
              <div>
                <p className="text-sm font-medium text-textSecondary">{header.eyebrow}</p>
                <h2 className="text-lg font-semibold sm:text-xl">{header.title}</h2>
              </div>
            </div>
            <div className="flex items-center gap-2 sm:gap-3">
              {hasPermission('notifications:read') ? (
                <Link
                  to="/notifications"
                  className="relative rounded-full border border-border p-2 text-textSecondary hover:bg-background"
                  aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
                >
                  <Bell className="h-5 w-5" />
                  {unread > 0 ? (
                    <span className="absolute -right-1 -top-1 min-w-[1.25rem] rounded-full bg-primary px-1 text-center text-[10px] font-semibold text-white">
                      {unread > 99 ? '99+' : unread}
                    </span>
                  ) : null}
                </Link>
              ) : null}
              <button
                type="button"
                className="rounded-full border border-border p-2 text-textSecondary hover:bg-background"
                aria-label="Sign out"
                onClick={handleLogout}
              >
                <LogOut className="h-5 w-5" />
              </button>
              <div className="hidden rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white sm:block">
                {user?.username ?? 'Portal'}
              </div>
            </div>
          </div>
        </header>

        <main className="p-4 sm:p-6">
          <PageTransition>
            <Outlet />
          </PageTransition>
        </main>
      </div>
    </div>
  );
}
