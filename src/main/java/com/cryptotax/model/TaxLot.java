package com.cryptotax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a remaining quantity of an asset purchased at a specific cost basis (FIFO lot).
 */
public class TaxLot {

    private BigDecimal remainingQuantity;
    private final BigDecimal costBasisPerUnit;
    private final LocalDateTime acquiredAt;

    public TaxLot(BigDecimal quantity, BigDecimal costBasisPerUnit, LocalDateTime acquiredAt) {
        this.remainingQuantity = quantity;
        this.costBasisPerUnit = costBasisPerUnit;
        this.acquiredAt = acquiredAt;
    }

    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public BigDecimal getCostBasisPerUnit()  { return costBasisPerUnit; }
    public LocalDateTime getAcquiredAt()     { return acquiredAt; }

    public void deduct(BigDecimal amount) {
        this.remainingQuantity = this.remainingQuantity.subtract(amount);
    }

    public boolean isExhausted() {
        return remainingQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }
}
