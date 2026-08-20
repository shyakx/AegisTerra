package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "climate_intel_jobs")
public class ClimateIntelJobEntity extends AuditableEntity {

    @Column(name = "job_number", nullable = false, length = 64)
    private String jobNumber;

    @Column(name = "job_type", nullable = false, length = 64)
    private String jobType;

    @Column(name = "params_json", columnDefinition = "text")
    private String paramsJson;

    @Column(name = "subjects_processed", nullable = false)
    private int subjectsProcessed;

    @Column(name = "subjects_failed", nullable = false)
    private int subjectsFailed;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    public int getSubjectsProcessed() { return subjectsProcessed; }
    public void setSubjectsProcessed(int subjectsProcessed) { this.subjectsProcessed = subjectsProcessed; }
    public int getSubjectsFailed() { return subjectsFailed; }
    public void setSubjectsFailed(int subjectsFailed) { this.subjectsFailed = subjectsFailed; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
