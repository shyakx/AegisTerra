# Domain Model

## Core Entities

### Farmer
Purpose: Represents an insured farmer or claimant in the system.
Relationships: One farmer may own many farms and many policies.
Lifecycle: Registration, verification, activation, suspension, deactivation.
Validation: National ID, phone number, and contact data must be present.
Status values: PENDING_VERIFICATION, ACTIVE, SUSPENDED, INACTIVE.
Business ownership: Farmer Operations.

### Farm
Purpose: Represents a farm belonging to a farmer.
Relationships: Many farms belong to one farmer; one farm may have many policies.
Lifecycle: Registration, validation, activation, update, archival.
Validation: Farm code, location, and size must be valid.
Status values: DRAFT, ACTIVE, INACTIVE.
Business ownership: Field Operations.

### FarmBoundary
Purpose: Stores the geospatial boundary of a farm.
Relationships: One farm has one or more boundaries.
Lifecycle: Capture, validation, update, removal.
Validation: Must be valid GeoJSON or polygon data.
Status values: DRAFT, ACTIVE, ARCHIVED.
Business ownership: GIS Operations.

### Policy
Purpose: Represents an insurance policy issued to a farmer for a farm.
Relationships: One policy belongs to one farmer and one farm; many claims may belong to one policy.
Lifecycle: Draft, issued, active, cancelled, expired.
Validation: Premium and coverage amounts must be valid and positive.
Status values: DRAFT, ISSUED, ACTIVE, CANCELLED, EXPIRED.
Business ownership: Underwriting.

### Claim
Purpose: Represents a claim filed under a policy.
Relationships: Many claims belong to one policy.
Lifecycle: Submitted, reviewed, approved, rejected, paid.
Validation: Claim amount, description, and evidence must be present.
Status values: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PAID.
Business ownership: Claims Operations.

### WeatherRecord
Purpose: Stores weather observations affecting risk.
Relationships: Related to geography and risk scoring.
Lifecycle: Ingestion, validation, aggregation, archival.
Validation: Timestamp and location must be present.
Status values: RECEIVED, VALIDATED, ARCHIVED.
Business ownership: Risk Intelligence.

### RiskScore
Purpose: Stores computed risk interpretation for a farm or region.
Relationships: Linked to farms and weather / satellite data.
Lifecycle: Computed, reviewed, published, deprecated.
Validation: Confidence score and model version must be present.
Status values: DRAFT, CALCULATED, REVIEWED, PUBLISHED.
Business ownership: Risk Intelligence.

### Notification
Purpose: Represents system or user notifications.
Relationships: Related to users and workflow events.
Lifecycle: Created, delivered, acknowledged, archived.
Status values: PENDING, DELIVERED, READ, ARCHIVED.
Business ownership: Operations.
