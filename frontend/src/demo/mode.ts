export function isStaticHostMode(): boolean {
  if (import.meta.env.VITE_DEMO_MODE === 'false') {
    return false;
  }
  if (import.meta.env.VITE_DEMO_MODE === 'true' || import.meta.env.DEV) {
    return true;
  }
  if (typeof window === 'undefined') {
    return false;
  }
  const host = window.location.hostname;
  return host.endsWith('.vercel.app') || host.endsWith('.vercel.sh');
}
