# Production Readiness Debt Register

This register captures the known release debt from the independent RC2 audit without turning future roadmap items into current blockers.

## DEMO BLOCKER

- None identified from the current baseline if the documented startup path is followed and the local PostGIS container is healthy.
- Any failure of the seeded demo data or a broken login path should be treated as a demo blocker and fixed immediately.

## PRODUCTION BLOCKER

- Production secrets and credential handling remain a blocker for live deployment. The current local/demo bootstrap credentials are not suitable for production exposure.
- TLS and deployment configuration are not documented as part of a full production deployment package.
- Backup/restore and rollback procedures are not captured as an operational runbook in the repository.
- Monitoring and observability runbooks are not yet documented for production operations.
- Scale testing and production-style resilience validation are not captured in the repository baseline.
- Provider integrations remain stubbed and should not be treated as production-ready without an explicit operational plan.

## FUTURE ENHANCEMENT

- Additional provider integrations for payments and external weather feeds.
- Stronger operational monitoring, alerting, and incident response procedures.
- Further UI and accessibility hardening beyond the current demo baseline.
- Additional automation around environment parity and release verification.
