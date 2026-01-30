package com.cryptotax.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResultDto(
        LocalDateTime date,
        String asset,
        BigDecimal quantitySold,
        BigDecimal proceeds,
        BigDecimal costBasis,
        BigDecimal gainLoss,
        String type
) {}
