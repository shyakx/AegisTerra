export const SITE_NAV = [
  { to: '/', label: 'Home' },
  { to: '/platform', label: 'Platform' },
  { to: '/partners', label: 'Partners' },
  { to: '/about', label: 'About' }
] as const;

export const HERO_MANDATES = ['insurers', 'banks', 'aggregators', 'ministries', 'development partners'];

export const CAPABILITIES = [
  {
    title: 'National dashboard',
    summary: 'One operating picture for farmers covered, active policies, premiums, claims paid, and growth.',
    points: [
      'High-level metrics with period-on-period change',
      'Drought, flood, pest, and heat-stress alerts at a glance',
      'Partner directory for insurers, banks, input suppliers, and public agencies'
    ]
  },
  {
    title: 'Farmer registry',
    summary: 'A verified record of smallholders, contact details, mobile money, and agro-ecological zone.',
    points: [
      'Profiles used by aggregators, insurers, and lenders',
      'Linkage to insured seed, fertilizer, and input loans',
      'Registration via Adjustera or an aggregator field agent'
    ]
  },
  {
    title: 'Risk intelligence',
    summary: 'Micro-climate zoning and AI alerts so risk is visible before losses are reconstructed.',
    points: [
      'Northern, Southern, Eastern, Western (Zones A–E) and Kigali (Zone F)',
      'Drought, Fall Armyworm, and crop-disease tracking',
      'Flags for field inspection and possible index-based payouts'
    ]
  },
  {
    title: 'Weather and climate',
    summary: 'Seasonal forecasts, satellite vegetation health, and a 15-year rainfall–yield baseline.',
    points: [
      'Rainfall, humidity, and temperature for the growing season',
      'NDVI telemetry by agro-ecological zone',
      'Historical comparison against the current season'
    ]
  },
  {
    title: 'Farm insights',
    summary: 'Live crop performance, planting progress, and digitized crop-cutting experiments.',
    points: [
      'NDVI, growth stage, and yield prediction (t/ha)',
      'Season completion by administrative sector',
      'Field sampling logs for harvest verification'
    ]
  },
  {
    title: 'Insured inputs',
    summary: 'Aggregators sell seed and fertilizer with an embedded premium on each bag.',
    points: [
      'Example: maize seed at USD 120/t uninsured versus USD 123/t insured',
      'Volumes sold with bundled cover',
      'Onboarding channel for farmer networks'
    ]
  },
  {
    title: 'Agricultural lending',
    summary: 'Banks see who they financed, plot location, crop, insurance status, and repayment risk.',
    points: [
      'Insured loans with a small premium embedded in the facility',
      'Crop condition and climate exposure on the loan book',
      'Payout status used in credit monitoring'
    ]
  },
  {
    title: 'Partner insurance',
    summary: 'AegisTerra does not underwrite. Insurers design products from climate reports, then run policy, claims, and payouts.',
    points: [
      'Product design and premium pricing from zonal risk',
      'Policy administration and claim review',
      'Portfolio monitoring for the insurer'
    ]
  },
  {
    title: 'Payouts',
    summary: 'Index triggers move money to farmers on mobile money within a defined window.',
    points: [
      'Transfers when zonal yields fall below historical benchmarks',
      'MTN and Airtel Money rails, target 14-day cycle',
      'History by peril: drought, flood, pest, disease'
    ]
  },
  {
    title: 'Analytics and reports',
    summary: 'Packages for insurers, lenders, ministries, and reinsurers — not a private operations notebook.',
    points: [
      'Triggered-payout notices for insurance companies',
      'Loan-repayment capacity views for banks and SACCOs',
      'CSV/PDF feeds for ministries and global reinsurers'
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
    body: 'Contribute farm, crop, and mobile-money details. Receive cover from partners, weather alerts, risk notices, and payout messages.'
  },
  {
    title: 'Insurance companies',
    body: 'Design and price products from climate risk reports. Collect premiums through banks and aggregators. Review claims and authorize payouts.'
  },
  {
    title: 'Financial institutions',
    body: 'Issue agricultural loans with insurance embedded. Monitor borrowers, crop condition, climate exposure, and payout status.'
  },
  {
    title: 'Aggregators',
    body: 'Register farmers and sell insured seed and fertilizer. The bag carries the premium; the network carries the risk picture.'
  },
  {
    title: 'Government',
    body: 'Consume aggregated food-security, productivity, coverage, and disaster-response intelligence.'
  },
  {
    title: 'Development partners',
    body: 'Use the same national statistics for programme monitoring and investment planning.'
  }
];
