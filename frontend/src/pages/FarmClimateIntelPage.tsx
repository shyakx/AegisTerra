import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function FarmClimateIntelPage() {
  const { farmId = '' } = useParams();
  const { hasPermission } = useAuth();
  const defaultTo = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const defaultFrom = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() - 90);
    return d.toISOString().slice(0, 10);
  }, []);
  const [from, setFrom] = useState(defaultFrom);
  const [to, setTo] = useState(defaultTo);

  const enabled = Boolean(farmId) && hasPermission('climate-intel:read');

  const riskQuery = useQuery({
    queryKey: ['farm-risk', farmId],
    queryFn: () => climateIntelApi.farmRiskScore(farmId),
    enabled,
    retry: false
  });

  const profileQuery = useQuery({
    queryKey: ['farm-profile', farmId],
    queryFn: () => climateIntelApi.farmProfile(farmId),
    enabled,
    retry: false
  });

  const timelineQuery = useQuery({
    queryKey: ['farm-timeline', farmId],
    queryFn: () => climateIntelApi.farmTimeline(farmId),
    enabled
  });

  const summaryQuery = useQuery({
    queryKey: ['farm-weather-summary', farmId, from, to],
    queryFn: () =>
      climateIntelApi.weatherSummary(farmId, {
        from: new Date(from).toISOString(),
        to: new Date(to + 'T23:59:59Z').toISOString()
      }),
    enabled: enabled && Boolean(from) && Boolean(to)
  });

  const seasonQuery = useQuery({
    queryKey: ['farm-season', farmId],
    queryFn: () => climateIntelApi.seasonSummary(farmId),
    enabled,
    retry: false
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Farm climate intelligence</p>
          <h1 className="text-3xl font-semibold">Farm risk & profile</h1>
          <p className="mt-1 font-mono text-sm text-textSecondary">{farmId}</p>
        </div>
        <Link to="/climate-intel" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          National dashboard
        </Link>
      </div>

      <section className="grid gap-4 rounded-2xl border border-border bg-surface p-6 sm:grid-cols-3">
        <Metric
          label="Risk score"
          value={
            riskQuery.data
              ? `${riskQuery.data.score.toFixed(1)} (${riskQuery.data.grade ?? '—'})`
              : riskQuery.isError
                ? 'Unavailable'
                : '…'
          }
        />
        <Metric
          label="Confidence"
          value={
            riskQuery.data?.confidence != null ? String(riskQuery.data.confidence) : '—'
          }
        />
        <Metric
          label="Rule"
          value={
            riskQuery.data
              ? `${riskQuery.data.ruleSetCode ?? riskQuery.data.modelVersion} ${riskQuery.data.ruleVersion ?? ''}`
              : '—'
          }
        />
      </section>

      {riskQuery.isError ? (
        <p className="text-sm text-textSecondary">
          {riskQuery.error instanceof ApiError ? riskQuery.error.message : 'No risk score yet'}
        </p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Weather summary</h2>
        <div className="mt-3 flex flex-wrap gap-3">
          <input
            type="date"
            className="rounded-xl border border-border px-3 py-2 text-sm"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <input
            type="date"
            className="rounded-xl border border-border px-3 py-2 text-sm"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>
        {summaryQuery.data ? (
          <div className="mt-4 space-y-2 text-sm">
            <p>{summaryQuery.data.narrative}</p>
            <p className="text-textSecondary">
              Rain {summaryQuery.data.rainfallTotalMm ?? '—'} mm · Temp {summaryQuery.data.meanTempC ?? '—'} °C ·
              Dry days {summaryQuery.data.dryDays ?? '—'} · Wet days {summaryQuery.data.wetDays ?? '—'}
            </p>
          </div>
        ) : summaryQuery.isError ? (
          <p className="mt-3 text-sm text-danger">
            {summaryQuery.error instanceof ApiError ? summaryQuery.error.message : 'Summary failed'}
          </p>
        ) : (
          <p className="mt-3 text-sm text-textSecondary">Loading summary…</p>
        )}
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Historical profile</h2>
          {profileQuery.data ? (
            <pre className="mt-3 max-h-64 overflow-auto rounded-xl bg-background p-3 text-xs">
              {prettyJson(profileQuery.data.snapshotJson)}
            </pre>
          ) : (
            <p className="mt-3 text-sm text-textSecondary">
              {profileQuery.isError ? 'No profile yet' : 'Loading…'}
            </p>
          )}
        </div>
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-lg font-semibold">Season summary</h2>
          {seasonQuery.data ? (
            <div className="mt-3 space-y-2 text-sm">
              <p>
                Grade: <strong>{seasonQuery.data.grade ?? '—'}</strong>
              </p>
              <p className="text-textSecondary">{seasonQuery.data.narrative ?? '—'}</p>
              <pre className="max-h-48 overflow-auto rounded-xl bg-background p-3 text-xs">
                {prettyJson(seasonQuery.data.metricsJson)}
              </pre>
            </div>
          ) : (
            <p className="mt-3 text-sm text-textSecondary">
              {seasonQuery.isError ? 'No season summary' : 'Loading…'}
            </p>
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-lg font-semibold">Climate timeline</h2>
        <ul className="mt-4 space-y-3">
          {(timelineQuery.data ?? []).map((item, idx) => (
            <li key={`${item.occurredAt}-${idx}`} className="border-b border-border/60 pb-3 text-sm">
              <p className="font-medium">
                {item.title} · {item.eventType}
              </p>
              <p className="text-textSecondary">
                {new Date(item.occurredAt).toLocaleString()}
                {item.severity ? ` · ${item.severity}` : ''}
              </p>
              {item.detail ? <p className="mt-1">{item.detail}</p> : null}
            </li>
          ))}
          {!timelineQuery.isLoading && (timelineQuery.data?.length ?? 0) === 0 ? (
            <li className="text-sm text-textSecondary">No timeline events</li>
          ) : null}
        </ul>
      </section>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm text-textSecondary">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  );
}

function prettyJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}
