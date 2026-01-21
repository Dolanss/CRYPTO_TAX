package com.cryptotax.service.strategy;

import com.cryptotax.exception.UnsupportedAssetException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AssetStrategyRegistry {

    private final Map<String, AssetStrategy> strategies;

    public AssetStrategyRegistry(List<AssetStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(AssetStrategy::getAssetSymbol, Function.identity()));
    }

    public AssetStrategy getStrategy(String asset) {
        AssetStrategy strategy = strategies.get(asset.toUpperCase());
        if (strategy == null) {
            throw new UnsupportedAssetException(asset);
        }
        return strategy;
    }

    public boolean isSupported(String asset) {
        return strategies.containsKey(asset.toUpperCase());
    }
}
