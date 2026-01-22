package com.cryptotax.service.strategy;

import org.springframework.stereotype.Component;

@Component
public class BtcStrategy implements AssetStrategy {
    @Override public String getAssetSymbol() { return "BTC"; }
    @Override public int getPrecision()       { return 8; }
}
