export const SITE_NAV = [
  { to: '/', label: 'Home' },
  { to: '/platform', label: 'Platform' },
  { to: '/partners', label: 'Partners' },
  { to: '/about', label: 'About' }
] as const;

export const HERO_MANDATES = ['insurers', 'banks', 'aggregators', 'ministries', 'development partners'];

export const CAPABILITIES = [
  {
    title: 'Past climate learning',
    summary: 'See how climate behaved over prior seasons by district and agroecological zone.',
    points: [
      'Station observations, imports, and seasonal summaries',
      'Comparable history for rainfall, heat, and stress indicators',
      'Shared facts for every stakeholder — not private insurer notebooks'
    ]
  },
  {
    title: 'Current risk picture',
    summary: 'Farm, district, AEZ, and national risk scores updated from trusted climate data.',
    points: [
      'Deterministic risk grades with rule lineage',
      'Open alerts where stress is rising now',
      'District and AEZ registry context for each place'
    ]
  },
  {
    title: 'Forward planning outlook',
    summary: 'Help insurers and partners know what is coming before unexpected harvest losses.',
    points: [
      'Past → now → ahead narrative for planning',
      'Highest-risk districts and AEZ rollups first',
      'Room to deepen with multi-year crop yield series'
    ]
  },
  {
    title: 'Farmer & farm registry',
    summary: 'Verified location, crops, and seasons so risk is always tied to real holdings.',
    points: [
      'Province → district location with derived AEZ',
      'Crop history on farms as the yield learning spine',
      'Registration for aggregators and field agents'
    ]
  },
  {
    title: 'Shared stakeholder views',
    summary: 'Insurers, banks, aggregators, government, and partners plan from one current dataset.',
    points: [
      'Role-appropriate dashboards without running claims desks here',
      'AegisTerra does not sell insurance or pay claims',
      'Partners consume intelligence; they remain product owners'
    ]
  }
];

export type NamedPartner = {
  name: string;
  /** Official mark from the institution’s site or Wikimedia Commons. Omitted when none is published. */
  logo?: string;
  /** White / light marks that need a dark well. */
  logoOnDark?: boolean;
  /** Shown in the tile when no public logo file exists. */
  initials?: string;
};

export const INSURERS: NamedPartner[] = [
  { name: 'Radiant Insurance Company', logo: '/partners/radiant.png' },
  { name: 'Sonarwa General Insurance', logo: '/partners/sonarwa.svg' },
  { name: 'Old Mutual Insurance Rwanda', logo: '/partners/old-mutual.png' },
  { name: 'BK Insurance', logo: '/partners/bk-insurance.png' },
  { name: 'Prime Insurance Ltd', logo: '/partners/prime.svg' },
  { name: 'ACRE Africa', logo: '/partners/acre.png' }
];

export const LENDERS: NamedPartner[] = [
  { name: 'Bank of Kigali (BK)', logo: '/partners/bk.jpg' },
  { name: 'BPR Bank Rwanda', logo: '/partners/bpr.jpg' },
  { name: 'Development Bank of Rwanda (BRD)', logo: '/partners/brd.svg', logoOnDark: true },
  { name: 'Urwego Bank', logo: '/partners/urwego.png' },
  { name: 'Umurenge SACCOs', logo: '/partners/rca.jpg' }
];

export const AGGREGATORS: NamedPartner[] = [
  { name: 'Seed Co Rwanda', logo: '/partners/seedco.png' },
  { name: 'Rwanda Fertilizer Company (RFC)', initials: 'RFC' },
  { name: 'Yara Rwanda', logo: '/partners/yara.svg' },
  { name: 'APTC Ltd (Agro-Processing Trust Corporation)', logo: '/partners/aptc.png' }
];

export const STAKEHOLDERS = [
  {
    title: 'Farmers',
    body: 'Share farm, crop, and location details. Receive climate alerts, risk notices, and guidance about what is coming.'
  },
  {
    title: 'Insurance companies',
    body: 'Plan products and portfolios from past climate behaviour, current risk, and outlook — before surprise harvest losses.'
  },
  {
    title: 'Financial institutions',
    body: 'See borrower farms and climate exposure when making or monitoring agricultural credit decisions.'
  },
  {
    title: 'Aggregators',
    body: 'Register farmer networks and follow climate risk across the holdings they serve.'
  },
  {
    title: 'Government',
    body: 'Consume aggregated food-security, productivity, and climate-risk intelligence.'
  },
  {
    title: 'Development partners',
    body: 'Use the same national statistics for programme monitoring and investment planning.'
  }
];
