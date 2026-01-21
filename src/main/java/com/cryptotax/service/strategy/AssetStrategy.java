package com.cryptotax.service.strategy;

import java.math.BigDecimal;

public interface AssetStrategy {
    String getAssetSymbol();

    /** Returns the minimum tradeable unit precision (decimal places). */
    int getPrecision();

    /** Validates that the quantity is within acceptable bounds for this asset. */
    default void validate(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getAssetSymbol() + ": quantity must be positive");
        }
    }
}
