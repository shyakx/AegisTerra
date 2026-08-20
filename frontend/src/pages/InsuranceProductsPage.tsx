import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { insuranceApi } from '../api/insurance';
import { ApiError } from '../api/client';

export default function InsuranceProductsPage() {
  const [productId, setProductId] = useState('');
  const productsQuery = useQuery({
    queryKey: ['insurance-products'],
    queryFn: () => insuranceApi.listProducts()
  });
  const typesQuery = useQuery({
    queryKey: ['policy-types'],
    queryFn: () => insuranceApi.listPolicyTypes()
  });
  const packagesQuery = useQuery({
    queryKey: ['coverage-packages', productId],
    queryFn: () => insuranceApi.listPackages(productId),
    enabled: Boolean(productId)
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-textSecondary">Insurance core</p>
          <h1 className="text-3xl font-semibold">Product catalog</h1>
        </div>
        <Link to="/policies" className="rounded-xl border border-border px-4 py-2 text-sm hover:bg-surface">
          Back to policies
        </Link>
      </div>

      {productsQuery.isError ? (
        <p className="text-sm text-red-600">
          {productsQuery.error instanceof ApiError ? productsQuery.error.message : 'Failed to load products'}
        </p>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2">
        {(productsQuery.data ?? []).map((product) => (
          <button
            key={product.id}
            type="button"
            onClick={() => setProductId(product.id)}
            className={`rounded-2xl border p-5 text-left transition ${
              productId === product.id ? 'border-primary bg-primary/5' : 'border-border bg-surface hover:border-primary/40'
            }`}
          >
            <p className="text-xs uppercase tracking-wide text-textSecondary">{product.code}</p>
            <h2 className="mt-1 text-xl font-semibold">{product.name}</h2>
            <p className="mt-2 text-sm text-textSecondary">{product.description ?? 'No description'}</p>
            <p className="mt-3 text-sm">
              Strategy <span className="font-medium">{product.pricingStrategyCode}</span> · {product.status}
            </p>
          </button>
        ))}
      </section>

      {productId ? (
        <section className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-2xl border border-border bg-surface p-6">
            <h3 className="text-lg font-semibold">Policy types</h3>
            <ul className="mt-4 space-y-2 text-sm">
              {(typesQuery.data ?? [])
                .filter((t) => t.productId === productId)
                .map((t) => (
                  <li key={t.id} className="rounded-xl border border-border bg-background px-3 py-2">
                    <span className="font-medium">{t.code}</span> — {t.name}
                  </li>
                ))}
            </ul>
          </div>
          <div className="rounded-2xl border border-border bg-surface p-6">
            <h3 className="text-lg font-semibold">Coverage packages</h3>
            <ul className="mt-4 space-y-2 text-sm">
              {(packagesQuery.data ?? []).map((p) => (
                <li key={p.id} className="rounded-xl border border-border bg-background px-3 py-2">
                  <span className="font-medium">{p.code}</span> — {p.name}
                  {p.coverageLevelPct != null ? ` · ${p.coverageLevelPct}%` : ''}
                  {p.maxSumInsured != null
                    ? ` · max ${Number(p.maxSumInsured).toLocaleString()}`
                    : ''}
                </li>
              ))}
              {packagesQuery.isLoading ? <li className="text-textSecondary">Loading…</li> : null}
            </ul>
          </div>
        </section>
      ) : (
        <p className="text-sm text-textSecondary">Select a product to view types and coverage packages.</p>
      )}
    </div>
  );
}
