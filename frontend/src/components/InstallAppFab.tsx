import { useEffect, useMemo, useState } from 'react';
import { Download, Share, X } from 'lucide-react';

const DISMISS_KEY = 'aegisterra.install-dismissed-until';
const DISMISS_MS = 14 * 24 * 60 * 60 * 1000;

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
};

function isStandalone(): boolean {
  const media = window.matchMedia('(display-mode: standalone)').matches;
  const iosStandalone = 'standalone' in navigator && Boolean((navigator as Navigator & { standalone?: boolean }).standalone);
  return media || iosStandalone;
}

function isIosSafari(): boolean {
  const ua = window.navigator.userAgent;
  const ios = /iPad|iPhone|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const safari = /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS/.test(ua);
  return ios && safari;
}

function dismissedUntil(): number {
  const raw = window.localStorage.getItem(DISMISS_KEY);
  const until = raw ? Number(raw) : 0;
  return Number.isFinite(until) ? until : 0;
}

export function InstallAppFab() {
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(null);
  const [openHelp, setOpenHelp] = useState(false);
  const [hidden, setHidden] = useState(true);
  const [ios, setIos] = useState(false);

  useEffect(() => {
    if (isStandalone() || dismissedUntil() > Date.now()) {
      setHidden(true);
      return;
    }

    setIos(isIosSafari());
    setHidden(false);

    const onPrompt = (event: Event) => {
      event.preventDefault();
      setDeferred(event as BeforeInstallPromptEvent);
      setHidden(false);
    };
    const onInstalled = () => {
      setDeferred(null);
      setHidden(true);
      setOpenHelp(false);
    };

    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  const label = useMemo(() => (ios && !deferred ? 'Add to Home Screen' : 'Install app'), [ios, deferred]);

  if (hidden) {
    return null;
  }

  function dismiss() {
    window.localStorage.setItem(DISMISS_KEY, String(Date.now() + DISMISS_MS));
    setHidden(true);
    setOpenHelp(false);
  }

  async function install() {
    if (deferred) {
      await deferred.prompt();
      const choice = await deferred.userChoice;
      setDeferred(null);
      if (choice.outcome === 'accepted') {
        setHidden(true);
      }
      return;
    }
    setOpenHelp(true);
  }

  return (
    <div className="pointer-events-none fixed bottom-5 right-5 z-50 flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      {openHelp ? (
        <div
          className="pointer-events-auto w-[min(22rem,calc(100vw-2.5rem))] rounded-2xl border border-border bg-white p-4 shadow-lift"
          role="dialog"
          aria-labelledby="install-help-title"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p id="install-help-title" className="font-display text-base font-semibold text-textPrimary">
                Install AegisTerra
              </p>
              <p className="mt-1 text-sm text-textSecondary">
                Keep climate risk intelligence on this device, like a native app.
              </p>
            </div>
            <button type="button" className="rounded-full p-1 text-textSecondary hover:bg-background" onClick={() => setOpenHelp(false)} aria-label="Close install help">
              <X className="h-4 w-4" />
            </button>
          </div>
          {ios ? (
            <ol className="mt-3 list-decimal space-y-1.5 pl-5 text-sm text-textPrimary">
              <li>
                Tap <Share className="inline h-3.5 w-3.5" aria-hidden /> Share
              </li>
              <li>Choose Add to Home Screen</li>
              <li>Confirm Add</li>
            </ol>
          ) : (
            <p className="mt-3 text-sm text-textPrimary">
              Use your browser menu and choose <span className="font-medium">Install app</span> or{' '}
              <span className="font-medium">Add to Home screen</span>.
            </p>
          )}
        </div>
      ) : null}

      <div className="pointer-events-auto flex items-center gap-1">
        <button
          type="button"
          onClick={() => void install()}
          className="at-btn inline-flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-semibold text-white shadow-lift"
          aria-label="Install AegisTerra application"
        >
          <Download className="h-4 w-4" aria-hidden />
          {label}
        </button>
        <button
          type="button"
          onClick={dismiss}
          className="rounded-full border border-border bg-white p-2 text-textSecondary shadow-lift hover:bg-background"
          aria-label="Dismiss install prompt"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
