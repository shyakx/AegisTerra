import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import { PoweredBy } from '../components/PoweredBy';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await forgotPassword(email);
      setSubmitted(true);
    } catch {
      setError('Unable to process the request. Please try again.');
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
        <h1 className="font-display mt-2 text-2xl font-semibold text-textPrimary">Reset your password</h1>
        <p className="mt-2 text-sm text-textSecondary">
          Enter your account email. If it exists, you will receive reset instructions.
        </p>

        {submitted ? (
          <div className="mt-6 space-y-4" role="status">
            <p className="text-sm text-textSecondary">
              If an account matches that email, a reset link has been issued. Check your inbox and follow the instructions.
            </p>
            <Link className="inline-block text-sm font-medium text-primary" to="/login">
              Back to sign-in
            </Link>
          </div>
        ) : (
          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            <div>
              <label className="mb-2 block text-sm font-medium" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                className="w-full rounded-xl bg-background px-4 py-3"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
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
              {loading ? 'Sending…' : 'Send reset link'}
            </button>
            <Link className="block text-center text-sm font-medium text-primary" to="/login">
              Back to sign-in
            </Link>
          </form>
        )}
      </div>
      <PoweredBy className="mt-6 text-center text-textSecondary" />
    </div>
  );
}
