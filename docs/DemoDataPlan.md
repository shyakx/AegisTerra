# AegisTerra Demo Data Plan

## Purpose

This document defines the next data package for the current RC2 demo baseline. It focuses on visual richness and operational coherence while staying within the existing backend, domain, and persistence architecture.

## Data principles

- Operational records shown in this demo are synthetic demonstration data unless explicitly identified as official statistics.
- Synthetic records must be inserted through the existing backend/domain/persistence path, preferably via Liquibase seed migrations or existing application services.
- Relationships must be coherent: household → farmer → farm → plot → crop season → policy → claim → assessment → settlement → ledger.
- Geographic distribution should span multiple Rwanda districts rather than clustering everything in Kigali.
- Do not create thousands of records; target a compact but visually rich sample that supports the demo narrative.

## Official reference data to prefer where practical

Where the architecture permits, future demo seeds should include official reference metadata from authoritative Rwanda sources such as NISR, MINAGRI, and official administrative/geographic references.

For each official statistic, store:

- source
- publication
- year
- indicator
- value
- unit

Suggested official reference datasets for later addition:

- district and sector administrative reference data
- crop production statistics by district and season
- seasonal climate normals or rainfall indicators
- published area and production benchmarks for common crops

## Synthetic operational data to generate

### 1. Households
- Entity: households
- Purpose: provide the social structure that anchors the farmer registry
- Source: synthetic demo data
- Data type: synthetic
- Record target: 12–15 households
- Relationships: one household may have one primary farmer and one or more linked farms
- Geographic coverage: Kigali, Musanze, Huye, Nyagatare
- Time coverage: current season and prior season
- How inserted: Liquibase seed changes with deterministic IDs and status values

### 2. Farmers
- Entity: farmers
- Purpose: populate the farmer registry and executive dashboard
- Source: synthetic demo data
- Data type: synthetic
- Record target: 12–20 farmers
- Relationships: household → farmer → farm
- Geographic coverage: 4–6 districts
- Time coverage: current crop season
- How inserted: Liquibase seed migration with linked household IDs and valid farmer codes

### 3. Farms and farm boundaries
- Entity: farms, farm_boundaries
- Purpose: support the farms list, GIS, and risk dashboards
- Source: synthetic demo data
- Data type: synthetic
- Record target: 12–18 farms
- Relationships: farmer → farm → boundary
- Geographic coverage: multiple districts and varied elevation bands
- Time coverage: current and previous season
- How inserted: Liquibase seed migration with geospatial polygon payloads and valid farm references

### 4. Plots
- Entity: plots
- Purpose: make the farm detail and plot pages look meaningful
- Source: synthetic demo data
- Data type: synthetic
- Record target: 2–3 plots per selected farm, or roughly 15–20 total
- Relationships: farm → plot
- Geographic coverage: distributed across selected districts
- Time coverage: current season
- How inserted: Liquibase seed migration with area and status values

### 5. Crop seasons
- Entity: crop_seasons
- Purpose: show planting, area, and crop history
- Source: synthetic demo data
- Data type: synthetic
- Record target: 10–15 crop season records
- Relationships: farm → plot → crop season
- Geographic coverage: maize, beans, potatoes, rice, cassava, banana
- Time coverage: current/previous season
- How inserted: existing crop and season catalog plus seed migration

### 6. Insurance products and packages
- Entity: insurance_products, policy_types, coverage_packages
- Purpose: make the insurance product catalog and calculator feel real
- Source: synthetic demo data plus existing catalog baseline
- Data type: synthetic/seeded catalog
- Record target: 2–3 products, 2–3 policy types, 2–3 packages
- Relationships: product → policy type → coverage package
- Geographic coverage: national product catalog
- Time coverage: current policy year
- How inserted: Liquibase seed migration backed by existing pricing rules and product catalog schema

### 7. Policies
- Entity: insurance_policies
- Purpose: populate policy portfolio, premium calculator, and claims lifecycle
- Source: synthetic demo data
- Data type: synthetic
- Record target: 8–10 policies
- Relationships: farmer → farm → policy → claim
- Geographic coverage: 4–6 districts
- Time coverage: current insurance year
- How inserted: seed migration using existing product/package IDs and a valid farmer/farm reference

### 8. Claims
- Entity: claims
- Purpose: demonstrate the claims workflow and portfolio views
- Source: synthetic demo data
- Data type: synthetic
- Record target: 6–8 claims
- Relationships: policy → claim → assessment/inspection
- Geographic coverage: at least 4 districts
- Time coverage: last 6 months
- How inserted: seed migration using valid policies and claim types

### 9. Claim assessment and inspection
- Entity: claim_assessments, claim_inspections
- Purpose: make the assessment and inspection screens look real
- Source: synthetic demo data
- Data type: synthetic
- Record target: 2–4 assessments and 2–4 inspections
- Relationships: claim → assessment/inspection
- Geographic coverage: distributed across the claim portfolio
- Time coverage: current claim cycle
- How inserted: seed migration after the claims are present

### 10. Settlements and ledger
- Entity: settlements, ledger_transactions, ledger_entries
- Purpose: show financial settlement progress and ledger state
- Source: synthetic demo data
- Data type: synthetic
- Record target: 3–5 settlements and 6–10 ledger entries
- Relationships: claim → settlement → ledger entries
- Geographic coverage: national demo portfolio
- Time coverage: current settlement window
- How inserted: seed migration linked to approved or payment-pending claims

### 11. Workflow tasks and decisions
- Entity: workflow_tasks, workflow_decision_records
- Purpose: make the task inbox and workflow experience meaningful during a demo
- Source: synthetic demo data
- Data type: synthetic
- Record target: 6–8 workflow tasks and 2–4 decision records
- Relationships: workflow task → subject (claim/settlement/policy) → decision record
- Geographic coverage: national operations
- Time coverage: current operations cycle
- How inserted: existing workflow services or Liquibase seed that references valid subjects

### 12. Notifications
- Entity: notifications
- Purpose: support the notification center and demonstrate operational communications
- Source: synthetic demo data
- Data type: synthetic
- Record target: 6–10 notifications
- Relationships: user → notification
- Geographic coverage: all relevant demo actors
- Time coverage: current week
- How inserted: existing notification engine or Liquibase seed linked to the admin user

### 13. Climate stations and observations
- Entity: weather_stations, weather_observations
- Purpose: populate the climate dashboard, GIS, and observation pages
- Source: synthetic demo data plus optional official station metadata
- Data type: synthetic/official hybrid
- Record target: 6–8 stations and a 14–30 day observation series for each
- Relationships: station → observations
- Geographic coverage: Kigali, Musanze, Huye, Nyagatare, and 2 additional districts
- Time coverage: 2–4 weeks of historical demo observations
- How inserted: Liquibase seed migration with station geometry and deterministic observation values

### 14. Climate datasets and alerts
- Entity: climate_datasets, climate_alerts, risk_scores, district_risk_snapshots
- Purpose: make the climate intelligence and risk pages believable
- Source: synthetic demo data plus official-style metadata labels
- Data type: synthetic/official hybrid
- Record target: 2–3 datasets, 4–8 alerts, 6–10 risk score records
- Relationships: farm/district → risk score → alert
- Geographic coverage: multiple districts
- Time coverage: current season and recent 30-day window
- How inserted: Liquibase seed migration and deterministic recalculation where supported by the existing risk engine

### 15. Users and audit history
- Entity: users, audit_logs
- Purpose: make the user and audit views look materially populated without overcomplicating the demo
- Source: synthetic demo data
- Data type: synthetic
- Record target: 3–4 users and 8–12 audit trail rows
- Relationships: actor user → audit log action
- Geographic coverage: national admin view
- Time coverage: current operations period
- How inserted: user provisioning plus audit events created through existing application flows

## Suggested insertion order

1. Seed the core agricultural and insurance backbone: households, farmers, farms, policies, claims
2. Add settlement and ledger examples for the financial story
3. Add workflow tasks and notifications for operational context
4. Enrich climate stations, observations, alerts, and risk scores for the climate narrative
5. Add user and audit data for governance visibility

## Data-quality guardrails

Before any seed expansion is considered complete, verify:

- no orphan farmers, farms, policies, claims, or settlements
- no impossible claim or policy states
- no broken foreign keys
- no duplicate identifiers
- total counts that reconcile with the executive dashboard and report views

## Demo labeling guidance

The application should remain professional and avoid cluttering the UI with obvious synthetic labels. The disclosure should be documented in:

- system/about information
- demo documentation
- data source documentation

The intended wording is:

> Operational records displayed in this demonstration are synthetic demonstration data unless explicitly identified as official statistics.
