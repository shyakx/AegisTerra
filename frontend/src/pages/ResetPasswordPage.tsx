import { FormEvent, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { resetPassword } from '../api/auth';
import { PoweredBy } from '../components/PoweredBy';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const tokenFromQuery = params.get('token') ?? '';
  const [token, setToken] = useState(tokenFromQuery);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await resetPassword({ token, newPassword });
      setDone(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message || 'Reset failed. The link may be invalid or expired.');
      } else {
        setError('Reset failed. Please request a new link.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4">
      <div className="w-full max-w-md rounded-3xl bg-surface p-8 text-textPrimary">
        <span className="mb-3 inline-block rounded-xl bg-white px-2 py-1">
          <img src="/aegisterra-logo.png" alt="AegisTerra" className="h-12 w-auto object-contain" />
        </span>
        <h1 className="font-display mt-2 text-2xl font-semibold">Choose a new password</h1>
        <p className="mt-2 text-sm text-textSecondary">
          Use at least 12 characters with upper, lower, digit, and special character.
        </p>

        {done ? (
          <div className="mt-6 space-y-3" role="status">
            <p className="text-sm">Password updated. You can sign in with your new credentials.</p>
            <Link className="text-sm font-medium text-primary hover:underline" to="/login">
              Go to sign-in
            </Link>
          </div>
        ) : (
          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            {!tokenFromQuery ? (
              <div>
                <label className="mb-2 block text-sm font-medium" htmlFor="token">
                  Reset token
                </label>
                <input
                  id="token"
                  required
                  className="w-full rounded-xl bg-background px-4 py-3"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                />
              </div>
            ) : null}
            <div>
              <label className="mb-2 block text-sm font-medium" htmlFor="newPassword">
                New password
              </label>
              <input
                id="newPassword"
                type="password"
                required
                autoComplete="new-password"
                className="w-full rounded-xl bg-background px-4 py-3"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium" htmlFor="confirmPassword">
                Confirm password
              </label>
              <input
                id="confirmPassword"
                type="password"
                required
                autoComplete="new-password"
                className="w-full rounded-xl bg-background px-4 py-3"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
            {error ? (
              <p className="text-sm text-danger" role="alert">
                {error}
              </p>
            ) : null}
            <button
              type="submit"
              disabled={loading}
              className="at-btn w-full rounded-xl bg-primary px-4 py-3 font-semibold text-white disabled:opacity-60"
            >
              {loading ? 'Updating…' : 'Update password'}
            </button>
          </form>
        )}
      </div>
      <PoweredBy className="mt-6 text-center text-textSecondary" />
    </div>
  );
}
