import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { AGGREGATORS, CAPABILITIES, HERO_MANDATES, INSURERS, LENDERS, STAKEHOLDERS } from '../../site/content';
import { PartnerBand } from '../../components/PartnerLogos';
import { PHOTOS } from '../../media/photos';
import { Reveal, Stagger, StaggerItem } from '../../motion/Reveal';
import { SignalBackdrop } from '../../visuals/SignalBackdrop';
import { RotatingPhrase } from '../../visuals/RotatingPhrase';

const CHAIN = [
  {
    title: 'Satellite',
    body: 'Earth-observation measures vegetation, rainfall, and heat stress across Rwanda’s agro-ecological zones.'
  },
  {
    title: 'Signal',
    body: 'Ground stations and weather feeds land as zonal climate intelligence — produced once, shared by mandate.'
  },
  {
    title: 'Devices',
    body: 'Operators, aggregators, and field agents work from one console and the Adjustera app.'
  },
  {
    title: 'Farms',
    body: 'Plots, crops, and harvest verification sit on the same record partners use to cover, lend, and pay.'
  }
];

export default function HomePage() {
  return (
    <div>
      <SignalBackdrop className="min-h-0">
        <div className="mx-auto max-w-6xl px-4 pt-24 text-white sm:px-6 lg:pt-32">
          <Reveal x={-24} y={0}>
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-200">
              Satellite · devices · farms
            </p>
            <h1 className="font-display mt-4 max-w-3xl text-4xl font-semibold leading-[1.08] tracking-tight sm:text-6xl">
              From spacecraft to the crop row.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-emerald-100">
              One picture of agricultural risk for <RotatingPhrase words={HERO_MANDATES} className="text-white" />.
            </p>
            <p className="mt-4 max-w-xl text-base leading-relaxed text-emerald-50 sm:text-lg">
              AegisTerra fuses Earth-observation imagery, ground stations, phones, and farm records so insurers, banks,
              aggregators, and government share one current. It does not sell insurance.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/join"
                className="at-btn inline-flex items-center gap-2 rounded-full bg-white px-6 py-3 text-sm font-semibold text-primary"
              >
                Join the platform
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link to="/login" className="inline-flex items-center rounded-full px-6 py-3 text-sm font-semibold text-white">
                Log in
              </Link>
            </div>
          </Reveal>
        </div>
        <div className="mx-auto max-w-6xl px-4 pb-20 pt-16 text-white sm:px-6 lg:pb-24">
          <div className="max-w-xl border-t border-white/15 pt-12">
            <Reveal>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">The live chain</p>
              <h2 className="font-display mt-2 text-3xl font-semibold sm:text-4xl">Orbit to the field</h2>
            </Reveal>
            <ol className="mt-10 grid gap-6">
              {CHAIN.map((item, index) => (
                <li key={item.title} className="flex gap-4">
                  <span className="font-display w-8 shrink-0 text-lg font-semibold text-emerald-200">
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <div>
                    <h3 className="text-lg font-semibold">{item.title}</h3>
                    <p className="mt-1 text-sm leading-relaxed text-emerald-50">{item.body}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </div>
      </SignalBackdrop>

      <section className="bg-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <Reveal>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Who it serves</p>
            <h2 className="font-display mt-2 text-3xl font-semibold sm:text-4xl">Six mandates. One current.</h2>
          </Reveal>
          <Stagger className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {STAKEHOLDERS.map((card) => (
              <StaggerItem key={card.title}>
                <article className="h-full rounded-2xl bg-background p-5">
                  <h3 className="text-lg font-semibold">{card.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-textSecondary">{card.body}</p>
                </article>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </section>

      <section className="bg-background">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <Reveal>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Capabilities</p>
              <h2 className="font-display mt-2 text-3xl font-semibold">What the live feed powers</h2>
            </Reveal>
            <Link to="/platform" className="text-sm font-semibold text-primary">
              Full platform map →
            </Link>
          </div>
          <div className="mt-10 grid items-stretch gap-8 lg:grid-cols-[1.05fr_0.95fr]">
            <SignalBackdrop photo={PHOTOS.satellite} kenBurns className="min-h-[360px] rounded-3xl">
              <div className="flex h-full min-h-[360px] flex-col justify-end px-8 py-8 text-white">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">Live feed</p>
                <p className="mt-2 max-w-md text-2xl font-semibold leading-snug">
                  Orbit, stations, and farm records as one current.
                </p>
              </div>
            </SignalBackdrop>
            <Stagger className="grid gap-4">
              {CAPABILITIES.slice(0, 4).map((item) => (
                <StaggerItem key={item.title}>
                  <article className="rounded-2xl bg-surface p-5">
                    <h3 className="text-lg font-semibold">{item.title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-textSecondary">{item.summary}</p>
                  </article>
                </StaggerItem>
              ))}
            </Stagger>
          </div>
        </div>
      </section>

      <section className="bg-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <Reveal>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Connected institutions</p>
            <h2 className="font-display mt-2 text-3xl font-semibold">Named partners on the network</h2>
          </Reveal>
          <div className="mt-10 space-y-10">
            <PartnerBand title="Insurance" partners={INSURERS} />
            <PartnerBand title="Finance" partners={LENDERS} />
            <PartnerBand title="Inputs" partners={AGGREGATORS} />
          </div>
          <div className="mt-10 flex flex-wrap gap-3">
            <Link to="/partners" className="at-btn rounded-full bg-primary px-5 py-3 text-sm font-semibold text-white">
              See the full directory
            </Link>
            <Link to="/join" className="rounded-full px-5 py-3 text-sm font-semibold text-primary">
              Request access
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
