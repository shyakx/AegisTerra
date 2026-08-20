package com.aegisterra.platform.infrastructure.persistence.climate;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "climate_import_jobs")
public class ClimateImportJobEntity extends AuditableEntity {
    @Column(name = "job_number", nullable = false, length = 64)
    private String jobNumber;
    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;
    @Column(name = "job_type", nullable = false, length = 64)
    private String jobType;
    @Column(name = "requested_window_start")
    private Instant requestedWindowStart;
    @Column(name = "requested_window_end")
    private Instant requestedWindowEnd;
    @Column(name = "params_json", columnDefinition = "text")
    private String paramsJson;
    @Column(name = "rows_read", nullable = false)
    private int rowsRead;
    @Column(name = "rows_accepted", nullable = false)
    private int rowsAccepted;
    @Column(name = "rows_rejected", nullable = false)
    private int rowsRejected;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "error_summary", length = 2000)
    private String errorSummary;
    @Column(name = "dataset_id")
    private UUID datasetId;

    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public Instant getRequestedWindowStart() { return requestedWindowStart; }
    public void setRequestedWindowStart(Instant requestedWindowStart) { this.requestedWindowStart = requestedWindowStart; }
    public Instant getRequestedWindowEnd() { return requestedWindowEnd; }
    public void setRequestedWindowEnd(Instant requestedWindowEnd) { this.requestedWindowEnd = requestedWindowEnd; }
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    public int getRowsRead() { return rowsRead; }
    public void setRowsRead(int rowsRead) { this.rowsRead = rowsRead; }
    public int getRowsAccepted() { return rowsAccepted; }
    public void setRowsAccepted(int rowsAccepted) { this.rowsAccepted = rowsAccepted; }
    public int getRowsRejected() { return rowsRejected; }
    public void setRowsRejected(int rowsRejected) { this.rowsRejected = rowsRejected; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
}
