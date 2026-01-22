package com.cryptotax.service.strategy;

import org.springframework.stereotype.Component;

@Component
public class EthStrategy implements AssetStrategy {
    @Override public String getAssetSymbol() { return "ETH"; }
    @Override public int getPrecision()       { return 8; }
}
