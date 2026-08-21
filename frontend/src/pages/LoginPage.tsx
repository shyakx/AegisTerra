import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { motion, useReducedMotion } from 'framer-motion';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { OPERATORS } from '../demo/catalog';
import { isStaticHostMode } from '../demo/mode';
import { roleLandingPath } from '../navigation/roleLanding';
import { PHOTOS } from '../media/photos';
import { SignalBackdrop } from '../visuals/SignalBackdrop';
import { easeOut } from '../motion/tokens';
import { PoweredBy } from '../components/PoweredBy';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const reduce = useReducedMotion();
  const { login, isAuthenticated, loading: authLoading, user } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const rawFrom = (location.state as { from?: string } | null)?.from;
  const publicPaths = new Set(['/', '/about', '/platform', '/partners', '/join', '/login']);
  const from = rawFrom && !publicPaths.has(rawFrom) ? rawFrom : undefined;

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
    <div className="relative flex min-h-screen flex-col items-center justify-center bg-background px-4 py-10">
      <motion.div
        className="relative mx-auto grid w-full max-w-5xl overflow-hidden rounded-3xl bg-surface lg:grid-cols-[1.1fr_0.9fr]"
        initial={reduce ? false : { y: 24 }}
        animate={{ y: 0 }}
        transition={{ duration: 0.4, ease: easeOut }}
      >
        <section className="relative hidden min-h-[560px] overflow-hidden lg:block">
          <SignalBackdrop photo={PHOTOS.satellite} blend="panel" className="h-full min-h-[560px]">
            <div className="flex h-full min-h-[560px] flex-col justify-between p-8 text-white">
              <Link to="/" className="inline-flex w-fit rounded-xl bg-white px-2 py-1">
                <img src="/aegisterra-logo.png" alt="AegisTerra" className="h-12 w-auto object-contain" />
              </Link>
              <div>
                <h1 className="font-display text-4xl font-semibold leading-tight">
                  Climate risk intelligence for agriculture
                </h1>
                <p className="mt-4 max-w-md text-sm text-emerald-50">
                  AegisTerra does not sell insurance. Satellite, ground-station, and farm records sit behind one verified
                  risk picture for partners.
                </p>
              </div>
            </div>
          </SignalBackdrop>
        </section>

        <section className="bg-surface p-8 sm:p-10 text-textPrimary">
          <div className="mb-8">
            <Link to="/" className="inline-flex rounded-xl bg-white px-2 py-1 lg:hidden">
              <img src="/aegisterra-logo.png" alt="AegisTerra" className="h-12 w-auto object-contain" />
            </Link>
            <h2 className="font-display mt-4 text-3xl font-semibold">Secure sign-in</h2>
            <p className="mt-2 text-sm text-textSecondary">Authorized operators enter the agricultural network.</p>
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
                className="w-full rounded-xl bg-background px-4 py-3"
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
                className="w-full rounded-xl bg-background px-4 py-3"
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
              <Link className="text-sm font-medium text-primary" to="/forgot-password">
                Forgot password?
              </Link>
            </div>

            {error ? (
              <p className="text-sm font-medium text-danger" role="alert">
                {error}
              </p>
            ) : null}

            <button
              className="at-btn w-full rounded-xl bg-primary px-4 py-3 font-semibold text-white disabled:opacity-60"
              type="submit"
              disabled={loading}
            >
              {loading ? 'Connecting…' : 'Enter workspace'}
            </button>
          </form>

          {isStaticHostMode() ? (
            <div className="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-3">
              {OPERATORS.map((account) => (
                <button
                  key={account.username}
                  type="button"
                  className="rounded-xl bg-background px-3 py-2 text-left text-xs"
                  onClick={() => {
                    setUsername(account.username);
                    setPassword(account.password);
                  }}
                >
                  <span className="block font-semibold">{account.label}</span>
                  <span className="text-textSecondary">{account.username}</span>
                </button>
              ))}
            </div>
          ) : null}

          <p className="mt-6 text-center text-sm text-textSecondary">
            New institution?{' '}
            <Link className="font-semibold text-primary" to="/join">
              Join the platform
            </Link>
            {' · '}
            <Link className="font-semibold text-primary" to="/">
              Back to home
            </Link>
          </p>
        </section>
      </motion.div>
      <PoweredBy className="relative mt-6 text-center text-textSecondary" />
    </div>
  );
}
