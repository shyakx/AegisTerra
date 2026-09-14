import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { climateIntelApi } from '../api/climateIntel';
import type { CropYieldOutlook, YieldClimateOutlook } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

/**
 * Owner-facing planning story: past yield/climate → recent → predicted outlook.
 */
export default function PlanningOutlookPage() {
  const { hasPermission } = useAuth();
  const enabled = hasPermission('climate-intel:read');

  const nationalQuery = useQuery({
    queryKey: ['planning-national'],
    queryFn: () => climateIntelApi.nationalDashboard(),
    enabled,
    retry: false
  });

  const aezQuery = useQuery({
    queryKey: ['planning-aez'],
    queryFn: () => climateIntelApi.aezRiskSummary(),
    enabled,
    retry: false
  });

  const yieldQuery = useQuery({
    queryKey: ['planning-yield-outlook'],
    queryFn: () => climateIntelApi.yieldClimateOutlook(),
    enabled,
    retry: false
  });

  const mlSpikeQuery = useQuery({
    queryKey: ['planning-ml-spike-maize'],
    queryFn: () => climateIntelApi.maizeYieldMlSpike(),
    enabled,
    retry: false
  });

  if (!enabled) {
    return (
      <div className="space-y-6">
        <div>
          <p className="text-sm text-textSecondary">Stakeholder planning</p>
          <h1 className="text-3xl font-semibold">Climate & yield outlook</h1>
          <p className="mt-2 max-w-3xl text-textSecondary">
            Past climate behaviour, current risk, and what is coming — so harvest losses are less of a surprise.
          </p>
        </div>
        <section className="rounded-2xl border border-border bg-surface p-6">
          <p className="text-sm text-textSecondary">
            Your account can open farms. Ask an operator for climate-intelligence access to see national and AEZ outlook
            numbers, or open your farm climate page from a farm detail screen.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <Link to="/farms" className="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white">
              My farms
            </Link>
            <Link to="/app" className="rounded-xl border border-border px-4 py-2 text-sm font-medium">
              Workspace home
            </Link>
          </div>
        </section>
      </div>
    );
  }

  const dash = nationalQuery.data;
  const aez = aezQuery.data;
  const yieldOutlook = yieldQuery.data;
  const zoneHeat = dash?.zoneHeat?.length ? dash.zoneHeat : aez?.zones ?? [];
  const districtHeat = dash?.districtHeat ?? [];
  const topDistricts = [...districtHeat].slice(0, 8);

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-textSecondary">Stakeholder planning</p>
        <h1 className="text-3xl font-semibold">Climate & yield outlook</h1>
        <p className="mt-2 max-w-3xl text-textSecondary">
          Insurers and partners use this view to reduce surprise harvest losses: what climate and yield did in the past,
          what risk looks like now, and what the outlook implies for the seasons ahead.
        </p>
      </div>

      <section className="grid gap-4 lg:grid-cols-3">
        <StoryCard
          step="1"
          title="Past"
          body={`Yield history (${yieldOutlook?.pastWindowStartYear ?? 'Y-10'}–${yieldOutlook?.pastWindowEndYear ?? 'Y-3'}) plus climate observations show how each crop performed when seasons behaved a certain way.`}
          link={{ to: '/climate', label: 'Open climate history' }}
        />
        <StoryCard
          step="2"
          title="Now"
          body={`Recent yield (${yieldOutlook?.recentWindowStartYear ?? 'Y-2'}–${yieldOutlook?.recentWindowEndYear ?? 'Y-1'}) and current climate risk (${
            yieldOutlook?.nationalMeanRiskScore != null
              ? `${yieldOutlook.nationalMeanRiskScore.toFixed(1)} · ${yieldOutlook.nationalRiskGrade ?? '—'}`
              : 'loading…'
          }) show the present posture.`}
          link={{ to: '/climate-intel', label: 'Open risk intelligence' }}
        />
        <StoryCard
          step="3"
          title="Ahead"
          body="Predicted yield per crop uses an explainable rule from recent trend dampened by national climate risk — so partners can plan before the next loss season."
          link={{ to: '/climate-intel/alerts', label: 'Open alerts' }}
        />
      </section>

      {nationalQuery.isError || aezQuery.isError || yieldQuery.isError ? (
        <p className="text-sm text-danger">
          {(nationalQuery.error instanceof ApiError && nationalQuery.error.message) ||
            (aezQuery.error instanceof ApiError && aezQuery.error.message) ||
            (yieldQuery.error instanceof ApiError && yieldQuery.error.message) ||
            'Unable to load planning data'}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">Crop yield outlook (past → recent → ahead)</h2>
            <p className="mt-1 text-sm text-textSecondary">
              Reference year {yieldOutlook?.referenceYear ?? '—'} · national climate risk{' '}
              {yieldOutlook?.nationalMeanRiskScore != null
                ? `${yieldOutlook.nationalMeanRiskScore.toFixed(1)} (${yieldOutlook.nationalRiskGrade ?? '—'})`
                : '—'}
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link to="/farms" className="text-sm font-medium text-primary hover:underline">
              Record yields on farm crop history
            </Link>
            {yieldOutlook ? (
              <button
                type="button"
                className="rounded-xl border border-border px-3 py-1.5 text-sm font-medium hover:bg-background"
                onClick={() => downloadYieldCsv(yieldOutlook)}
              >
                Export CSV
              </button>
            ) : null}
          </div>
        </div>

        <div className="mt-4 space-y-4">
          {(yieldOutlook?.crops ?? []).map((crop) => (
            <CropOutlookCard key={crop.cropCode} crop={crop} />
          ))}
          {!yieldQuery.isLoading && (yieldOutlook?.crops?.length ?? 0) === 0 ? (
            <p className="text-sm text-textSecondary">
              No harvested yield records yet. Add yield (t/ha) on farm crop-history seasons to unlock this outlook.
            </p>
          ) : null}
        </div>

        {yieldOutlook?.methodology ? (
          <p className="mt-4 text-xs text-textSecondary">{yieldOutlook.methodology}</p>
        ) : null}
      </section>

      <section className="rounded-2xl border border-dashed border-border bg-surface p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-textSecondary">Experimental · ADR-011</p>
            <h2 className="text-lg font-semibold">Maize ML spike (Stage A)</h2>
            <p className="mt-1 text-sm text-textSecondary">
              Feature extract + leave-one-year-out MAE. Does not replace the rule outlook above.
            </p>
          </div>
        </div>

        {mlSpikeQuery.isError ? (
          <p className="mt-3 text-sm text-danger">
            {mlSpikeQuery.error instanceof ApiError ? mlSpikeQuery.error.message : 'ML spike failed'}
          </p>
        ) : null}

        {mlSpikeQuery.data ? (
          <div className="mt-4 space-y-4">
            <p className="text-sm">
              <span className="font-semibold">Verdict:</span> {mlSpikeQuery.data.verdict}
            </p>
            <div className="grid gap-3 sm:grid-cols-3">
              <Metric
                label="MAE naive (last year)"
                value={fmtYield(mlSpikeQuery.data.metrics.maeNaiveLastYear)}
                hint="t/ha"
              />
              <Metric
                label="MAE rule (recent mean)"
                value={fmtYield(mlSpikeQuery.data.metrics.maeRuleRecentMean)}
                hint="t/ha"
              />
              <Metric
                label="MAE linear (lag+rain)"
                value={fmtYield(mlSpikeQuery.data.metrics.maeLinearLagRain)}
                hint="t/ha"
              />
            </div>
            <p className="text-sm text-textSecondary">
              Best method: <strong>{mlSpikeQuery.data.metrics.bestMethod}</strong>
              {' · '}
              {mlSpikeQuery.data.featureRowCount} yearly feature rows · {mlSpikeQuery.data.metrics.folds} folds
            </p>
            <p className="text-xs text-textSecondary">{mlSpikeQuery.data.notes}</p>
          </div>
        ) : (
          <p className="mt-3 text-sm text-textSecondary">{mlSpikeQuery.isLoading ? 'Evaluating spike…' : 'No spike data'}</p>
        )}
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Agroecological risk (now)</h2>
          <p className="mt-1 text-sm text-textSecondary">
            Mean risk by zone from latest district snapshots. Higher grades mean higher expected climate stress.
          </p>
          <ul className="mt-4 space-y-3">
            {zoneHeat.map((z) => (
              <li key={z.code} className="flex items-center justify-between gap-3 border-b border-border/60 pb-3 text-sm">
                <div>
                  <p className="font-medium">
                    Zone {z.code}
                    {z.name ? ` — ${z.name}` : ''}
                  </p>
                  <p className="text-textSecondary">
                    Score {z.meanScore != null ? z.meanScore.toFixed(1) : '—'}
                    {z.districtCount != null ? ` · ${z.districtCount} districts` : ''}
                  </p>
                </div>
                <span className="rounded-lg bg-background px-3 py-1 font-semibold">{z.grade ?? '—'}</span>
              </li>
            ))}
            {!nationalQuery.isLoading && !aezQuery.isLoading && zoneHeat.length === 0 ? (
              <li className="text-sm text-textSecondary">No zone rollups yet — import climate data and recalculate risk.</li>
            ) : null}
          </ul>
        </div>

        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Highest-risk districts (now)</h2>
          <p className="mt-1 text-sm text-textSecondary">
            Places insurers and partners should watch first for the coming season.
          </p>
          <ul className="mt-4 space-y-3">
            {topDistricts.map((d) => (
              <li key={d.districtCode} className="flex items-center justify-between gap-3 border-b border-border/60 pb-3 text-sm">
                <Link className="font-medium text-primary hover:underline" to={`/climate-intel/districts/${d.districtCode}`}>
                  {d.districtCode}
                </Link>
                <span>
                  {d.meanScore != null ? d.meanScore.toFixed(1) : '—'} · {d.grade ?? '—'}
                </span>
              </li>
            ))}
            {!nationalQuery.isLoading && topDistricts.length === 0 ? (
              <li className="text-sm text-textSecondary">No district heat yet.</li>
            ) : null}
          </ul>
        </div>
      </section>
    </div>
  );
}

function CropOutlookCard({ crop }: { crop: CropYieldOutlook }) {
  const maxYield = Math.max(
    ...crop.yearlySeries.map((p) => p.meanYieldTHa ?? 0),
    crop.predictedYieldTHa ?? 0,
    0.01
  );

  return (
    <article className="rounded-xl border border-border/80 bg-background p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold">
            {crop.cropName} <span className="text-textSecondary">({crop.cropCode})</span>
          </h3>
          <p className="mt-1 text-sm text-textSecondary">{crop.narrative}</p>
        </div>
        <span className="rounded-lg border border-border px-3 py-1 text-xs font-semibold tracking-wide">
          {crop.outlookLabel.replace(/_/g, ' ')}
        </span>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <Metric label="Past mean" value={fmtYield(crop.pastMeanYieldTHa)} hint="t/ha" />
        <Metric label="Recent mean" value={fmtYield(crop.recentMeanYieldTHa)} hint="t/ha" />
        <Metric label="Predicted next" value={fmtYield(crop.predictedYieldTHa)} hint="t/ha" />
      </div>

      <div className="mt-4">
        <p className="text-xs font-medium uppercase tracking-wide text-textSecondary">Yearly series</p>
        <div className="mt-2 flex h-16 items-end gap-1">
          {crop.yearlySeries.map((point) => {
            const h = point.meanYieldTHa == null ? 4 : Math.max(8, Math.round((point.meanYieldTHa / maxYield) * 64));
            return (
              <div key={point.year} className="flex flex-1 flex-col items-center justify-end gap-1" title={`${point.year}: ${fmtYield(point.meanYieldTHa)} t/ha`}>
                <div className="w-full rounded-t bg-primary/80" style={{ height: `${h}px` }} />
                <span className="text-[10px] text-textSecondary">{String(point.year).slice(2)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </article>
  );
}

function Metric({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="rounded-lg border border-border bg-surface px-3 py-2">
      <p className="text-xs text-textSecondary">{label}</p>
      <p className="mt-1 text-lg font-semibold">
        {value} <span className="text-xs font-normal text-textSecondary">{hint}</span>
      </p>
    </div>
  );
}

function fmtYield(value: number | null | undefined) {
  if (value == null) return '—';
  return value.toFixed(2);
}

function downloadYieldCsv(outlook: YieldClimateOutlook) {
  const header = [
    'crop_code',
    'crop_name',
    'past_mean_yield_t_ha',
    'recent_mean_yield_t_ha',
    'predicted_yield_t_ha',
    'yield_change_pct_recent_vs_past',
    'outlook_label',
    'farm_count',
    'season_sample_count',
    'national_mean_risk_score',
    'national_risk_grade',
    'reference_year'
  ].join(',');
  const rows = outlook.crops.map((c) =>
    [
      c.cropCode,
      csvEscape(c.cropName),
      c.pastMeanYieldTHa ?? '',
      c.recentMeanYieldTHa ?? '',
      c.predictedYieldTHa ?? '',
      c.yieldChangePctRecentVsPast ?? '',
      c.outlookLabel,
      c.farmCount,
      c.seasonSampleCount,
      outlook.nationalMeanRiskScore ?? '',
      outlook.nationalRiskGrade ?? '',
      outlook.referenceYear
    ].join(',')
  );
  const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `aegisterra-yield-outlook-${outlook.referenceYear}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function csvEscape(value: string) {
  if (value.includes(',') || value.includes('"')) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

function StoryCard({
  step,
  title,
  body,
  link
}: {
  step: string;
  title: string;
  body: string;
  link: { to: string; label: string };
}) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-6">
      <p className="text-xs font-semibold uppercase tracking-wider text-textSecondary">Step {step}</p>
      <h2 className="mt-1 text-xl font-semibold">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-textSecondary">{body}</p>
      <Link to={link.to} className="mt-4 inline-block text-sm font-medium text-primary hover:underline">
        {link.label}
      </Link>
    </div>
  );
}
