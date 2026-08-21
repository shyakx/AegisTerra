import type { NamedPartner } from '../site/content';

type BandProps = {
  title: string;
  description?: string;
  partners: NamedPartner[];
};

export function PartnerBand({ title, description, partners }: BandProps) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase tracking-[0.18em] text-textSecondary">{title}</h3>
      {description ? <p className="mt-1 max-w-2xl text-sm text-textSecondary">{description}</p> : null}
      <PartnerLogos partners={partners} />
    </div>
  );
}

export function PartnerLogos({ partners }: { partners: NamedPartner[] }) {
  const cols =
    partners.length >= 6
      ? 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-6'
      : partners.length === 5
        ? 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-5'
        : 'grid-cols-2 sm:grid-cols-4';

  return (
    <ul className={`mt-5 grid gap-3 ${cols}`}>
      {partners.map((partner) => (
        <li key={partner.name}>
          <article className="flex h-full min-h-[8.5rem] flex-col items-center rounded-2xl bg-background px-3 py-4">
            <span className="flex h-14 w-full items-center justify-center">
              {partner.logo ? (
                partner.logoOnDark ? (
                  <span className="rounded-md bg-sidebar px-3 py-2">
                    <img src={partner.logo} alt="" className="max-h-8 max-w-[6.5rem] object-contain" />
                  </span>
                ) : (
                  <img src={partner.logo} alt="" className="max-h-10 max-w-[7.5rem] object-contain" />
                )
              ) : (
                <span className="font-display text-sm font-semibold tracking-wide text-primary">
                  {partner.initials ?? '—'}
                </span>
              )}
            </span>
            <p className="mt-3 line-clamp-2 text-center text-xs font-medium leading-snug text-textPrimary">
              {partner.name}
            </p>
          </article>
        </li>
      ))}
    </ul>
  );
}
