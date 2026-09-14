import { Link } from 'react-router-dom';
import { CAPABILITIES } from '../../site/content';
import { PHOTOS } from '../../media/photos';
import { Reveal, Stagger, StaggerItem } from '../../motion/Reveal';
import { SignalBackdrop } from '../../visuals/SignalBackdrop';

export default function PlatformPage() {
  return (
    <div className="bg-background">
      <SignalBackdrop photo={PHOTOS.satellite} className="min-h-0">
        <div className="mx-auto max-w-6xl px-4 py-20 text-white sm:px-6 lg:py-24">
          <Reveal x={-28} y={0}>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">Platform</p>
            <h1 className="font-display mt-3 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
              Past climate and yield learning → plans that avoid surprise losses.
            </h1>
            <p className="mt-4 max-w-xl text-lg leading-relaxed text-emerald-50">
              One shared picture of what happened, what is happening, and what is coming — for insurers, farmers, banks,
              aggregators, and public partners. AegisTerra does not sell insurance.
            </p>
          </Reveal>
        </div>
      </SignalBackdrop>

      <ol className="mx-auto max-w-6xl space-y-4 px-4 py-16 sm:px-6">
        {CAPABILITIES.map((item, index) => (
          <Reveal key={item.title} y={24} delay={index * 0.02}>
            <li className="grid gap-6 rounded-2xl bg-surface p-6 md:grid-cols-[4rem_1fr] md:p-8">
              <span className="font-display text-xl font-semibold text-primary">{String(index + 1).padStart(2, '0')}</span>
              <div>
                <h2 className="text-2xl font-semibold text-textPrimary">{item.title}</h2>
                <p className="mt-2 leading-relaxed text-textSecondary">{item.summary}</p>
                <ul className="mt-4 space-y-2 text-sm text-textPrimary">
                  {item.points.map((point) => (
                    <li key={point} className="flex gap-3">
                      <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                      {point}
                    </li>
                  ))}
                </ul>
              </div>
            </li>
          </Reveal>
        ))}
      </ol>

      <Stagger className="mx-auto flex max-w-6xl flex-wrap gap-3 px-4 pb-16 sm:px-6">
        <StaggerItem>
          <Link to="/join" className="at-btn inline-flex rounded-full bg-primary px-5 py-3 text-sm font-semibold text-white">
            Join the platform
          </Link>
        </StaggerItem>
        <StaggerItem>
          <Link to="/login" className="inline-flex rounded-full px-5 py-3 text-sm font-semibold text-primary">
            Log in
          </Link>
        </StaggerItem>
      </Stagger>
    </div>
  );
}
