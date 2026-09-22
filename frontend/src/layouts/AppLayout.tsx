import { useMemo, useState } from 'react';
import { Outlet, NavLink, useNavigate, Link } from 'react-router-dom';
import { LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { buildNavigation, portalHeader } from '../navigation/buildNavigation';
import { PageTransition } from '../motion/PageTransition';
import { PoweredBy } from '../components/PoweredBy';

const ROLE_LABELS: Record<string, string> = {
  SYSTEM_ADMIN: 'System administrator',
  INSURANCE_ADMIN: 'Insurance administrator',
  INSURANCE_OFFICER: 'Insurance officer',
  FI_OFFICER: 'Bank officer',
  GOVERNMENT_ANALYST: 'Government analyst',
  DEVELOPMENT_PARTNER: 'Development partner',
  AGGREGATOR: 'Aggregator officer',
  FARMER: 'Farmer',
  AUDITOR: 'Auditor',
  SUPPORT: 'Support agent'
};

function roleLabel(role: string | undefined): string {
  return role ? ROLE_LABELS[role] ?? role.replace(/_/g, ' ').toLowerCase() : 'Portal user';
}

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout, hasPermission, hasRole, hasAnyPermission } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const header = portalHeader(user?.roles);
  const identityName = user?.displayName || user?.username || 'Signed in';
  const identityRole = roleLabel(user?.roles?.[0]);

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
            <p className="text-sm font-medium">{identityName}</p>
            <p className="mt-0.5 text-xs text-emerald-100/80">{identityRole}</p>
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
              <button
                type="button"
                className="inline-flex items-center gap-2 rounded-full border border-border px-3 py-2 text-sm font-medium text-textSecondary hover:bg-background"
                aria-label="Sign out"
                onClick={handleLogout}
              >
                <LogOut className="h-5 w-5" />
                <span>Sign out</span>
              </button>
              <div className="rounded-full bg-primary px-3 py-2 text-sm font-semibold text-white sm:px-4">
                <span>{identityName}</span>
                <span className="mx-1.5 text-white/60">|</span>
                <span>{identityRole}</span>
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
