import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { OPERATORS } from '../demo/catalog';
import { isStaticHostMode } from '../demo/mode';
import { roleLandingPath } from '../navigation/roleLanding';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, loading: authLoading, user } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: string } | null)?.from;

  if (!authLoading && isAuthenticated) {
    return <Navigate to={from || roleLandingPath(user?.roles)} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await login({ username: username.trim(), password, rememberMe });
      navigate(from || roleLandingPath(undefined), { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('Invalid credentials. Please try again.');
      } else if (err instanceof ApiError && err.status === 423) {
        setError('Account is locked. Try again later or contact an administrator.');
      } else if (err instanceof ApiError && err.status === 429) {
        setError('Too many attempts. Please wait and try again.');
      } else {
        setError('Unable to sign in. Check that the API is reachable.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="relative flex min-h-screen items-center justify-center px-4 py-10"
      style={{
        background:
          'radial-gradient(circle at 12% 18%, rgba(16,185,129,0.18), transparent 36%), radial-gradient(circle at 88% 12%, rgba(14,116,144,0.16), transparent 32%), linear-gradient(160deg, #ecfdf5 0%, #f8fafc 42%, #e2e8f0 100%)'
      }}
    >
      <div className="grid w-full max-w-5xl overflow-hidden rounded-3xl border border-border bg-surface shadow-lg lg:grid-cols-[1.05fr_0.95fr]">
        <section className="relative hidden bg-sidebar p-10 text-white lg:block">
          <div
            className="absolute inset-0 opacity-30"
            style={{
              backgroundImage:
                'linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px)',
              backgroundSize: '42px 42px'
            }}
          />
          <div className="relative z-10 flex h-full flex-col justify-between">
            <div>
              <img
                src="/aegisterra-logo.png"
                alt="AegisTerra"
                className="h-16 w-auto object-contain"
              />
              <h1 className="mt-6 text-4xl font-semibold leading-tight">
                National agricultural insurance operations
              </h1>
              <p className="mt-4 max-w-md text-sm text-emerald-50/85">
                Secure access to farmer registry, policies, claims, settlements, and climate intelligence for
                government and insurer partners.
              </p>
            </div>
            <ul className="space-y-2 text-sm text-emerald-100/90">
              <li>• Enterprise RBAC and audit-ready workflows</li>
              <li>• Climate-informed risk and settlement operations</li>
              <li>• National coverage for farmers, insurers, and government</li>
            </ul>
          </div>
        </section>

        <section className="p-8 sm:p-10">
          <div className="mb-8 lg:hidden">
            <img
              src="/aegisterra-logo.png"
              alt="AegisTerra"
              className="h-12 w-auto object-contain"
            />
            <h1 className="mt-3 text-2xl font-semibold text-textPrimary">Secure sign-in</h1>
          </div>
          <div className="mb-8 hidden lg:block">
            <img
              src="/aegisterra-logo.png"
              alt="AegisTerra"
              className="mb-4 h-12 w-auto object-contain"
            />
            <h2 className="text-2xl font-semibold text-textPrimary">Secure sign-in</h2>
            <p className="mt-2 text-sm text-textSecondary">
              Access the national agricultural insurance operations portal.
            </p>
          </div>

          <form className="space-y-4" onSubmit={handleSubmit} noValidate>
            <div>
              <label className="mb-2 block text-sm font-medium text-textPrimary" htmlFor="username">
                Username
              </label>
              <input
                id="username"
                name="username"
                required
                className="w-full rounded-xl border border-border bg-background px-4 py-3 outline-none focus:border-primary"
                value={username}
                autoComplete="username"
                onChange={(event) => setUsername(event.target.value)}
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium text-textPrimary" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                name="password"
                required
                className="w-full rounded-xl border border-border bg-background px-4 py-3 outline-none focus:border-primary"
                type="password"
                value={password}
                autoComplete="current-password"
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>

            <div className="flex items-center justify-between gap-3">
              <label className="flex items-center gap-2 text-sm text-textSecondary" htmlFor="rememberMe">
                <input
                  id="rememberMe"
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => setRememberMe(event.target.checked)}
                />
                Remember me
              </label>
              <Link className="text-sm font-medium text-primary hover:underline" to="/forgot-password">
                Forgot password?
              </Link>
            </div>

            {error ? (
              <p className="text-sm font-medium text-danger" role="alert">
                {error}
              </p>
            ) : null}

            <button
              className="w-full rounded-xl bg-primary px-4 py-3 font-semibold text-white transition hover:bg-secondary disabled:opacity-60"
              type="submit"
              disabled={loading}
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          {isStaticHostMode() ? (
            <div className="mt-6">
              <p className="mb-2 text-xs font-medium uppercase tracking-wide text-textSecondary">Sign in as</p>
              <div className="grid grid-cols-2 gap-2">
                {OPERATORS.map((account) => (
                  <button
                    key={account.username}
                    type="button"
                    className="rounded-xl border border-border px-3 py-2 text-left text-xs hover:border-primary"
                    onClick={() => {
                      setUsername(account.username);
                      setPassword(account.password);
                    }}
                  >
                    <span className="block font-semibold text-textPrimary">{account.label}</span>
                    <span className="text-textSecondary">{account.username}</span>
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          <p className="mt-6 text-xs text-textSecondary">
            Authorized personnel only. Session cookies are HttpOnly; credentials are never stored in localStorage.
          </p>
        </section>
      </div>
    </div>
  );
}
