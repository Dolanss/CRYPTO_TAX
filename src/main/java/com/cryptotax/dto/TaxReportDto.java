package com.cryptotax.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TaxReportDto(
        List<TradeResultDto> trades,
        BigDecimal totalRealizedGains,
        BigDecimal totalRealizedLosses,
        BigDecimal netPosition,
        Map<String, BigDecimal> remainingPositions
) {}
