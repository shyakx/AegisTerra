import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { climateApi } from '../api/climate';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const sampleCsv = `observed_at,variable_code,value,unit
2026-08-01T06:00:00Z,RAIN_MM,12.5,mm
2026-08-01T06:00:00Z,TEMP_C,22.1,C
2026-08-02T06:00:00Z,RAIN_MM,0.0,mm
2026-08-02T06:00:00Z,TEMP_C,23.4,C`;

export default function ClimateImportJobsPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const [stationCode, setStationCode] = useState('KGL-01');
  const [csvContent, setCsvContent] = useState(sampleCsv);

  const jobsQuery = useQuery({
    queryKey: ['climate-import-jobs'],
    queryFn: () => climateApi.listImportJobs({ page: 0, size: 50 }),
    enabled: hasPermission('climate:read')
  });

  const startMutation = useMutation({
    mutationFn: () =>
      climateApi.startImportJob({
        providerCode: 'MANUAL',
        jobType: 'FILE_UPLOAD',
        stationCode,
        csvContent
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['climate-import-jobs'] })
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Climate data</p>
          <h1 className="text-3xl font-semibold">Import jobs</h1>
        </div>
        <Link to="/climate" className="rounded-xl border border-border px-4 py-2 text-sm font-medium hover:bg-surface">
          Dashboard
        </Link>
      </div>

      {hasPermission('climate:import') || hasPermission('climate:write') ? (
        <form
          className="space-y-3 rounded-2xl border border-border bg-surface p-6"
          onSubmit={(e) => {
            e.preventDefault();
            startMutation.mutate();
          }}
        >
          <h2 className="text-lg font-semibold">Manual / CSV import</h2>
          <label className="block text-sm">
            <span className="mb-1 block text-textSecondary">Station code</span>
            <input
              className="w-full max-w-md rounded-xl border border-border px-3 py-2"
              value={stationCode}
              onChange={(e) => setStationCode(e.target.value)}
              required
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-textSecondary">CSV content</span>
            <textarea
              className="min-h-40 w-full rounded-xl border border-border px-3 py-2 font-mono text-xs"
              value={csvContent}
              onChange={(e) => setCsvContent(e.target.value)}
              required
            />
          </label>
          {startMutation.isError ? (
            <p className="text-sm text-danger" role="alert">
              {startMutation.error instanceof ApiError ? startMutation.error.message : 'Import failed'}
            </p>
          ) : null}
          {startMutation.isSuccess ? (
            <p className="text-sm text-success">Started {startMutation.data.jobNumber}</p>
          ) : null}
          <button
            type="submit"
            disabled={startMutation.isPending}
            className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
          >
            {startMutation.isPending ? 'Importing…' : 'Start import'}
          </button>
        </form>
      ) : null}

      {jobsQuery.isError ? (
        <p className="text-sm text-danger" role="alert">
          {jobsQuery.error instanceof ApiError ? jobsQuery.error.message : 'Failed to load jobs'}
        </p>
      ) : null}

      <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border text-textSecondary">
            <tr>
              <th className="px-4 py-3 font-medium">Job</th>
              <th className="px-4 py-3 font-medium">Provider</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Accepted</th>
              <th className="px-4 py-3 font-medium">Rejected</th>
              <th className="px-4 py-3 font-medium">Created</th>
            </tr>
          </thead>
          <tbody>
            {(jobsQuery.data?.content ?? []).map((job) => (
              <tr key={job.id} className="border-b border-border/60">
                <td className="px-4 py-3 font-medium">{job.jobNumber}</td>
                <td className="px-4 py-3">{job.providerCode}</td>
                <td className="px-4 py-3">{job.status}</td>
                <td className="px-4 py-3">{job.rowsAccepted}</td>
                <td className="px-4 py-3">{job.rowsRejected}</td>
                <td className="px-4 py-3">{new Date(job.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
