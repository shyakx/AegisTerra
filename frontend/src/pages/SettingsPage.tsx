import { useMutation } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { changePassword, updateProfile } from '../api/auth';
import { PageHeader } from '../components/PageHeader';
import { useAuth } from '../auth/AuthContext';

const CHECKS: Array<{ label: string; path: string; note: string; permission?: string }> = [
  { label: 'Dashboard', path: '/app', note: 'Role overview' },
  { label: 'Climate hub', path: '/climate-hub', note: 'History, forecast, risk', permission: 'climate:read' },
  {
    label: 'System intelligence',
    path: '/system-intelligence',
    note: 'Planning brain',
    permission: 'climate-intel:read'
  },
  {
    label: 'Agro-ecological zones',
    path: '/agroecological-zones',
    note: 'Zones and sub-zones',
    permission: 'farmers:read'
  },
  { label: 'Analytical reports', path: '/reports', note: 'Report entry points' },
  { label: 'Policies', path: '/policies', note: 'Partner policies', permission: 'policies:read' },
  { label: 'Payouts', path: '/payouts', note: 'Payout notifications', permission: 'settlements:read' },
  { label: 'Farmers & loans', path: '/lending', note: 'Bank loan book', permission: 'loans:read' }
];

export default function SettingsPage() {
  const { user, refreshUser, hasPermission } = useAuth();
  const [profile, setProfile] = useState({ email: '', displayName: '' });
  const [password, setPassword] = useState({ currentPassword: '', newPassword: '' });
  const [message, setMessage] = useState<string | null>(null);

  const visibleChecks = CHECKS.filter((item) => !item.permission || hasPermission(item.permission)).map((item) => {
    if (item.path === '/farmers' && user?.farmerId) {
      return { ...item, path: `/farmers/${user.farmerId}`, note: 'Your linked farmer record' };
    }
    return item;
  });

  useEffect(() => {
    if (user) {
      setProfile({ email: user.email ?? '', displayName: user.displayName ?? '' });
    }
  }, [user]);

  const profileMutation = useMutation({
    mutationFn: () =>
      updateProfile({
        email: profile.email.trim(),
        displayName: profile.displayName.trim()
      }),
    onSuccess: async () => {
      setMessage('Profile updated');
      await refreshUser();
    },
    onError: (err) => setMessage(err instanceof ApiError ? err.message : 'Profile update failed')
  });

  const passwordMutation = useMutation({
    mutationFn: () =>
      changePassword({
        currentPassword: password.currentPassword,
        newPassword: password.newPassword
      }),
    onSuccess: () => {
      setMessage('Password changed');
      setPassword({ currentPassword: '', newPassword: '' });
    },
    onError: (err) => setMessage(err instanceof ApiError ? err.message : 'Password change failed')
  });

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Account"
        title="Platform information"
        description="Product identity, your profile, and password settings."
      />

      {message ? (
        <p className="rounded-xl border border-border bg-surface px-4 py-3 text-sm" role="status">
          {message}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Build</h2>
        <dl className="mt-4 grid gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-xs uppercase tracking-wide text-textSecondary">Product</dt>
            <dd className="mt-1 text-sm font-medium">AegisTerra Climate Risk Intelligence</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-textSecondary">Release</dt>
            <dd className="mt-1 text-sm font-medium">1.0</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-textSecondary">Version</dt>
            <dd className="mt-1 text-sm font-medium">1.0.0</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-textSecondary">Architecture</dt>
            <dd className="mt-1 text-sm font-medium">Modular monolith · Clean Architecture</dd>
          </div>
        </dl>
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <form
          className="space-y-3 rounded-2xl border border-border bg-surface p-6"
          onSubmit={(e) => {
            e.preventDefault();
            profileMutation.mutate();
          }}
        >
          <h2 className="text-lg font-semibold">My profile</h2>
          <p className="text-sm text-textSecondary">Username: {user?.username}</p>
          <input
            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
            value={profile.displayName}
            onChange={(e) => setProfile({ ...profile, displayName: e.target.value })}
            placeholder="Display name"
          />
          <input
            type="email"
            required
            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
            value={profile.email}
            onChange={(e) => setProfile({ ...profile, email: e.target.value })}
            placeholder="Email"
          />
          <button type="submit" className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white">
            Save profile
          </button>
        </form>

        <form
          className="space-y-3 rounded-2xl border border-border bg-surface p-6"
          onSubmit={(e) => {
            e.preventDefault();
            passwordMutation.mutate();
          }}
        >
          <h2 className="text-lg font-semibold">Change password</h2>
          <input
            type="password"
            required
            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
            value={password.currentPassword}
            onChange={(e) => setPassword({ ...password, currentPassword: e.target.value })}
            placeholder="Current password"
          />
          <input
            type="password"
            required
            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
            value={password.newPassword}
            onChange={(e) => setPassword({ ...password, newPassword: e.target.value })}
            placeholder="New password"
          />
          <button type="submit" className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white">
            Update password
          </button>
        </form>
      </section>

      {visibleChecks.length > 0 ? (
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Module shortcuts</h2>
        <p className="mt-1 text-sm text-textSecondary">Areas available for your current role.</p>
        <ul className="mt-4 divide-y divide-border">
          {visibleChecks.map((item) => (
            <li key={item.path} className="flex flex-wrap items-center justify-between gap-2 py-3">
              <div>
                <p className="text-sm font-medium">{item.label}</p>
                <p className="text-xs text-textSecondary">{item.note}</p>
              </div>
              <Link to={item.path} className="rounded-xl border border-border px-3 py-1.5 text-sm hover:bg-background">
                Open
              </Link>
            </li>
          ))}
        </ul>
      </section>
      ) : null}
    </div>
  );
}
