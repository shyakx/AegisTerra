export function isStaticHostMode(): boolean {
  if (import.meta.env.VITE_DEMO_MODE === 'true') {
    return true;
  }
  if (typeof window === 'undefined') {
    return false;
  }
  const host = window.location.hostname;
  return host.endsWith('.vercel.app') || host.endsWith('.vercel.sh');
}
