# Maize yield ML spike (Stage A)

**Status:** Experimental spike — not a production forecast  
**Date:** 2026-09-14  
**Related:** ADR-010 product focus · yield outlook on `/planning`

## Goal

Prove we can:

1. Extract a **tabular feature matrix** from existing yield + climate tables  
2. Compare a **simple linear model** against the explainable rule baseline  
3. Decide whether Stage B (batch inference) is warranted  

## Endpoint

`GET /api/v1/climate-intel/planning/ml-spike/maize`  
Permission: `climate-intel:read`

## Features (per harvest year, MAIZE)

| Feature | Source |
|---|---|
| `meanYieldTHa` | `crop_seasons.yield_t_ha` × seasons |
| `lag1YieldTHa` / `lag2YieldTHa` | prior year means |
| `seasonRainMm` | `climate_observation_aggregates` SEASON / `ML_SPIKE_DEMO` |
| `yearIndex` | year − 2016 |

Demo rain series is migration `036` (`source = ML_SPIKE_DEMO`) — **not** official met office truth.

## Evaluation

Leave-one-year-out MAE for:

- **Naive** — last year’s yield  
- **Rule** — mean of prior 2 years (close to planning “recent”)  
- **Linear** — OLS `yield ~ lag1 + season_rain` (falls back to lag1-only)

## Verdict field

- `LINEAR_BEATS_RULE` → candidate for Stage B after real data volume grows  
- `RULE_HOLDS` → keep `/planning` rule as default  
- `INSUFFICIENT_DATA` → need more harvested seasons  

## Explicit non-goals

- No model registry / artifact store yet  
- No training inside HTTP request beyond this tiny OLS  
- No replacement of rule outlook on the main planning cards  
- No Claims / premium pricing coupling  

## Stage B (later)

Only if linear (or a stronger offline model) beats the rule on **real** multi-year yield + climate — then add versioned batch inference and dual display (rule + ML).
