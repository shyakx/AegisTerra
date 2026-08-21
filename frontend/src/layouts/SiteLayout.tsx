import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X, ArrowRight } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { SITE_NAV } from '../site/content';
import { PHOTOS } from '../media/photos';
import { PageTransition } from '../motion/PageTransition';
import { JourneyRail } from '../visuals/JourneyRail';
import { SignalBackdrop } from '../visuals/SignalBackdrop';
import { PoweredBy } from '../components/PoweredBy';

export default function SiteLayout() {
  const { isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <div className="site-shell min-h-screen bg-background font-sans text-textPrimary">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-white focus:px-3 focus:py-2"
      >
        Skip to content
      </a>
      <header className="sticky top-0 z-40 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link to="/" className="flex items-center gap-3" onClick={() => setOpen(false)}>
            <img src="/aegisterra-logo.png" alt="AegisTerra" className="h-10 w-auto object-contain" />
          </Link>
          <nav className="hidden items-center gap-7 text-sm font-medium text-textSecondary lg:flex" aria-label="Site">
            {SITE_NAV.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => (isActive ? 'text-primary' : 'hover:text-primary')}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="hidden items-center gap-2 lg:flex">
            {isAuthenticated ? (
              <Link to="/app" className="at-btn rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white">
                Open workspace
              </Link>
            ) : (
              <>
                <Link to="/login" className="rounded-full px-4 py-2 text-sm font-semibold text-primary">
                  Log in
                </Link>
                <Link to="/join" className="at-btn rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white">
                  Join the platform
                </Link>
              </>
            )}
          </div>
          <button
            type="button"
            className="rounded-full p-2 text-primary lg:hidden"
            aria-label={open ? 'Close menu' : 'Open menu'}
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
        {open ? (
          <div className="bg-white px-4 py-4 lg:hidden">
            <nav className="flex flex-col gap-3 text-sm font-medium" aria-label="Mobile">
              {SITE_NAV.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.to === '/'} onClick={() => setOpen(false)}>
                  {item.label}
                </NavLink>
              ))}
              {isAuthenticated ? (
                <Link to="/app" onClick={() => setOpen(false)} className="text-primary">
                  Open workspace
                </Link>
              ) : (
                <>
                  <Link to="/login" onClick={() => setOpen(false)}>
                    Log in
                  </Link>
                  <Link to="/join" onClick={() => setOpen(false)} className="text-primary">
                    Join the platform
                  </Link>
                </>
              )}
            </nav>
          </div>
        ) : null}
      </header>
      <JourneyRail />
      <div id="main">
        <PageTransition>
          <Outlet />
        </PageTransition>
      </div>
      <footer>
        <SignalBackdrop photo={PHOTOS.groundStation} className="min-h-0" kenBurns={false}>
          <div className="mx-auto max-w-6xl px-4 py-14 text-white sm:px-6">
            <div className="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
              <div className="max-w-lg">
                <span className="inline-block rounded-lg bg-white px-2.5 py-1.5">
                  <img src="/aegisterra-logo.png" alt="" className="h-9 w-auto object-contain" />
                </span>
                <p className="mt-5 text-sm leading-relaxed text-emerald-50">
                  Satellite intelligence for Rwandan agriculture. AegisTerra does not sell insurance.
                </p>
                <nav className="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-sm text-emerald-100" aria-label="Footer">
                  <Link to="/platform" className="hover:text-white">
                    Platform
                  </Link>
                  <Link to="/partners" className="hover:text-white">
                    Partners
                  </Link>
                  <Link to="/about" className="hover:text-white">
                    About
                  </Link>
                  <Link to="/login" className="hover:text-white">
                    Log in
                  </Link>
                </nav>
              </div>
              {isAuthenticated ? (
                <Link
                  to="/app"
                  className="at-btn inline-flex w-fit items-center gap-2 rounded-full bg-white px-5 py-2.5 text-sm font-semibold text-primary"
                >
                  Open workspace
                  <ArrowRight className="h-4 w-4" />
                </Link>
              ) : (
                <Link
                  to="/join"
                  className="at-btn inline-flex w-fit items-center gap-2 rounded-full bg-white px-5 py-2.5 text-sm font-semibold text-primary"
                >
                  Join the platform
                  <ArrowRight className="h-4 w-4" />
                </Link>
              )}
            </div>
            <div className="mt-12 flex items-center justify-between gap-4">
              <p className="text-[11px] text-emerald-200/80">AegisTerra</p>
              <PoweredBy className="text-emerald-100" />
            </div>
          </div>
        </SignalBackdrop>
      </footer>
    </div>
  );
}
