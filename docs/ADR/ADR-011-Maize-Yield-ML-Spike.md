# ADR-011: Experimental maize yield ML spike (Stage A)

## Status

Accepted (experimental)

## Date

2026-09-14

## Context

ADR-010 keeps the product on explainable climate/yield planning. Stakeholders asked what ML would take; Stage A is a **spike** only: feature extract + leave-one-year-out metrics for maize, without shipping a production model.

Climate Intelligence 8B remains rule-first. Demo seasonal rain aggregates (`036`, source `ML_SPIKE_DEMO`) exist only to exercise climate joins.

## Decision

1. Expose `GET /api/v1/climate-intel/planning/ml-spike/maize` for operators with `climate-intel:read`.  
2. Compare naive / rule / simple OLS MAE; report a verdict.  
3. Do **not** replace `/planning` rule outlook until a later ADR promotes a model.  
4. Do **not** introduce Python/MLOps stacks in this spike.

## Consequences

### Positive

- Concrete path from data → metrics without overbuilding  
- Rule baseline stays the trusted product surface  

### Negative / costs

- Demo rain is synthetic; verdicts on demo data are indicative only  
- Tiny in-process OLS is not a model platform  

## Alternatives considered

| Alternative | Why not now |
|---|---|
| Full sklearn + MLflow | Too heavy before real yield volume |
| Replace rule UI with ML | Violates ADR-010 trust/explainability |
