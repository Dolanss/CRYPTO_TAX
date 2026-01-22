package com.cryptotax.service.strategy;

import org.springframework.stereotype.Component;

@Component
public class SolStrategy implements AssetStrategy {
    @Override public String getAssetSymbol() { return "SOL"; }
    @Override public int getPrecision()       { return 9; }
}
