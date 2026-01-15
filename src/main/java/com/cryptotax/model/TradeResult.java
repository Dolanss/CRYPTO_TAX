package com.cryptotax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResult(
        LocalDateTime date,
        String asset,
        BigDecimal quantitySold,
        BigDecimal proceeds,
        BigDecimal costBasis,
        BigDecimal gainLoss
) {
    public boolean isGain() {
        return gainLoss.compareTo(BigDecimal.ZERO) > 0;
    }
}
