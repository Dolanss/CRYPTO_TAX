package com.cryptotax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
        LocalDateTime date,
        TransactionType type,
        String asset,
        BigDecimal quantity,
        BigDecimal priceUsd,
        BigDecimal feeUsd
) {}
