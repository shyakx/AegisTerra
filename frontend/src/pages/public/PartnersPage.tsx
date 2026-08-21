import { Link } from 'react-router-dom';
import { AGGREGATORS, INSURERS, LENDERS, STAKEHOLDERS } from '../../site/content';
import { PartnerBand } from '../../components/PartnerLogos';
import { PHOTOS } from '../../media/photos';
import { Reveal, Stagger, StaggerItem } from '../../motion/Reveal';
import { SignalBackdrop } from '../../visuals/SignalBackdrop';

export default function PartnersPage() {
  return (
    <div className="bg-background">
      <SignalBackdrop photo={PHOTOS.devicesTeam} className="min-h-0">
        <div className="mx-auto max-w-6xl px-4 py-20 text-white sm:px-6 lg:py-24">
          <Reveal x={-24} y={0}>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-200">Partners</p>
            <h1 className="font-display mt-3 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
              Institutions as live nodes
            </h1>
            <p className="mt-4 max-w-xl text-lg leading-relaxed text-emerald-50">
              Insurers underwrite. Banks lend. Aggregators onboard farmers and sell insured inputs. Government and
              development partners read the national picture — from the same current.
            </p>
          </Reveal>
        </div>
      </SignalBackdrop>

      <Stagger className="mx-auto mt-12 grid max-w-6xl gap-4 px-4 md:grid-cols-2 sm:px-6">
        {STAKEHOLDERS.map((item) => (
          <StaggerItem key={item.title}>
            <article className="h-full rounded-2xl bg-surface p-6">
              <h2 className="text-lg font-semibold">{item.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-textSecondary">{item.body}</p>
            </article>
          </StaggerItem>
        ))}
      </Stagger>

      <section className="mx-auto max-w-6xl space-y-12 px-4 py-16 sm:px-6">
        <PartnerBand
          title="Insurance companies"
          description="Receive climate risk reports, design products, and authorize payouts."
          partners={INSURERS}
        />
        <PartnerBand
          title="Financial institutions"
          description="Farmers financed, loan amount, plot, crop, and repayment capacity."
          partners={LENDERS}
        />
        <PartnerBand
          title="Input aggregators"
          description="Insured versus uninsured tonnes — maize seed at USD 120/t or USD 123/t with cover embedded."
          partners={AGGREGATORS}
        />
        <Link to="/join" className="at-btn inline-flex rounded-full bg-primary px-5 py-3 text-sm font-semibold text-white">
          Become a partner
        </Link>
      </section>
    </div>
  );
}

