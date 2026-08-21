import { Link } from 'react-router-dom';
import { PHOTOS } from '../../media/photos';
import { Reveal } from '../../motion/Reveal';
import { SignalBackdrop } from '../../visuals/SignalBackdrop';

export default function AboutPage() {
  return (
    <div className="bg-background">
      <SignalBackdrop photo={PHOTOS.farmerField} className="min-h-0">
        <div className="mx-auto max-w-6xl px-4 py-20 text-white sm:px-6 lg:py-24">
          <Reveal x={-24} y={0}>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">About</p>
            <h1 className="font-display mt-3 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
              The national backbone for agricultural risk
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-emerald-50">
              AegisTerra is a climate risk intelligence platform. Satellites, ground stations, and the Adjustera mobile
              app lock farmers, insurers, banks, aggregators, and government onto one verified current.
            </p>
            <p className="mt-4 max-w-xl leading-relaxed text-emerald-100">
              It does not provide insurance. Localized intelligence helps partners design cover, lend, and manage climate
              exposure in farmer networks.
            </p>
          </Reveal>
        </div>
      </SignalBackdrop>

      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <Reveal>
          <h2 className="font-display text-2xl font-bold">How farmers enter the system</h2>
          <p className="mt-3 leading-relaxed text-textSecondary">
            Farmers register location, crops, and agro-ecological zone through Adjustera. Aggregators and field agents
            may assist. That profile is the basis for climate-informed services offered by partner institutions.
          </p>
        </Reveal>
        <Reveal delay={0.08}>
          <h2 className="font-display mt-12 text-2xl font-bold">What we optimize for</h2>
          <ul className="mt-4 space-y-4 text-textSecondary">
            <li>
              <strong className="text-primary">Farmer inclusion</strong> — a single registry of farms, boundaries,
              production history, and cover.
            </li>
            <li>
              <strong className="text-primary">Climate monitoring</strong> — weather, drought, flood, pest, disease, and
              vegetation health as they develop.
            </li>
            <li>
              <strong className="text-primary">Ahead-of-loss assessment</strong> — AI and historical yields estimate risk
              before paperwork reconstructs it.
            </li>
            <li>
              <strong className="text-primary">Partner insurance automation</strong> — policy, claims, and payout workflows
              owned by insurers, run on shared data.
            </li>
            <li>
              <strong className="text-primary">Credit risk reduction</strong> — banks see insurance status, crop condition,
              and climate exposure.
            </li>
            <li>
              <strong className="text-primary">Public transparency</strong> — ministries receive timely agricultural
              statistics.
            </li>
          </ul>
        </Reveal>
        <Reveal delay={0.12}>
          <h2 className="font-display mt-12 text-2xl font-bold">Data ownership</h2>
          <p className="mt-3 leading-relaxed text-textSecondary">
            The platform is a trusted intermediary, not the data owner. Farmer profiles belong to aggregators and
            farmers; policies belong to insurers; loans belong to financial institutions; weather and satellite feeds
            belong to their providers; risk scores are produced by the engine; national statistics belong to government.
          </p>
        </Reveal>
        <div className="mt-10 flex flex-wrap gap-3">
          <Link to="/join" className="at-btn rounded-full bg-primary px-5 py-3 text-sm font-semibold text-white">
            Join the platform
          </Link>
          <Link to="/platform" className="rounded-full px-5 py-3 text-sm font-semibold text-primary">
            Read the platform map
          </Link>
        </div>
      </div>
    </div>
  );
}
