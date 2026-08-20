# AegisTerra Release Baseline Report

**Release:** AegisTerra 1.0.0-RC2
**Implementation version:** 1.0.0-rc.2

## Scope

This document captures a stable, reproducible baseline for the current RC2 demo implementation. No business modules, workflows, or domain logic were changed as part of this baseline work.

## Version evidence

- Backend version: 1.0.0-rc.2 in [backend/pom.xml](../backend/pom.xml)
- Frontend version: 1.0.0-rc.2 in [frontend/package.json](../frontend/package.json)
- Application metadata: 1.0.0-rc.2 in [backend/src/main/resources/application.yml](../backend/src/main/resources/application.yml)

## Baseline posture

- Demo seed is provided by Liquibase changelog 020 and is intended to be reproducible when the local PostGIS container and backend are started from a clean state.
- The current implementation is suitable for a controlled competition/demo environment, not for production deployment.
- The workspace currently does not expose Git metadata from the CLI, so the git baseline must be recorded from the actual repository location if/when the project is placed under Git version control.

## Baseline documents

- [DEMO-RC2-STARTUP.md](DEMO-RC2-STARTUP.md)
- [DEMO-RC2-SCRIPT.md](DEMO-RC2-SCRIPT.md)
- [DEMO-RC2-SMOKE-TEST.md](DEMO-RC2-SMOKE-TEST.md)
- [ProductionReadinessDebt.md](ProductionReadinessDebt.md)
