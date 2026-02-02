package com.cryptotax.exception;

public class UnsupportedAssetException extends RuntimeException {
    public UnsupportedAssetException(String asset) {
        super("Unsupported asset: " + asset + ". Supported assets: BTC, ETH, SOL");
    }
}
