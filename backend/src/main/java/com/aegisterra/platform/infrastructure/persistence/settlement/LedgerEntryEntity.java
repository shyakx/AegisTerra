package com.aegisterra.platform.infrastructure.persistence.settlement;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity extends AuditableEntity {

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "settlement_id")
    private UUID settlementId;

    @Column(name = "entry_no", nullable = false)
    private int entryNo;

    @Column(name = "entry_type", nullable = false, length = 32)
    private String entryType;

    @Column(name = "account_code", nullable = false, length = 64)
    private String accountCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 2000)
    private String narration;

    @Column(name = "external_statement_line_id", length = 128)
    private String externalStatementLineId;

    @Column(name = "reconciliation_ref", length = 128)
    private String reconciliationRef;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public void setLedgerTransactionId(UUID ledgerTransactionId) { this.ledgerTransactionId = ledgerTransactionId; }
    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public int getEntryNo() { return entryNo; }
    public void setEntryNo(int entryNo) { this.entryNo = entryNo; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getExternalStatementLineId() { return externalStatementLineId; }
    public void setExternalStatementLineId(String externalStatementLineId) { this.externalStatementLineId = externalStatementLineId; }
    public String getReconciliationRef() { return reconciliationRef; }
    public void setReconciliationRef(String reconciliationRef) { this.reconciliationRef = reconciliationRef; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
