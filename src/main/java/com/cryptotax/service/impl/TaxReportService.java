package com.cryptotax.service.impl;

import com.cryptotax.dto.TaxReportDto;
import com.cryptotax.dto.TradeResultDto;
import com.cryptotax.model.TaxLot;
import com.cryptotax.model.TradeResult;
import com.cryptotax.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaxReportService {

    private final FifoCalculatorService fifoCalculator;

    public TaxReportService(FifoCalculatorService fifoCalculator) {
        this.fifoCalculator = fifoCalculator;
    }

    public TaxReportDto buildReport(List<Transaction> transactions) {
        Map<String, Deque<TaxLot>> openLots = new LinkedHashMap<>();
        List<TradeResult> tradeResults = fifoCalculator.calculate(transactions, openLots);

        BigDecimal totalGains = tradeResults.stream()
                .map(TradeResult::gainLoss)
                .filter(gl -> gl.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLosses = tradeResults.stream()
                .map(TradeResult::gainLoss)
                .filter(gl -> gl.compareTo(BigDecimal.ZERO) < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPosition = totalGains.add(totalLosses);

        Map<String, BigDecimal> remainingPositions = openLots.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(TaxLot::getRemainingQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(8, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<TradeResultDto> tradeDtos = tradeResults.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new TaxReportDto(
                tradeDtos,
                totalGains.setScale(2, RoundingMode.HALF_UP),
                totalLosses.setScale(2, RoundingMode.HALF_UP),
                netPosition.setScale(2, RoundingMode.HALF_UP),
                remainingPositions
        );
    }

    private TradeResultDto toDto(TradeResult r) {
        return new TradeResultDto(
                r.date(),
                r.asset(),
                r.quantitySold().setScale(8, RoundingMode.HALF_UP),
                r.proceeds().setScale(2, RoundingMode.HALF_UP),
                r.costBasis().setScale(2, RoundingMode.HALF_UP),
                r.gainLoss().setScale(2, RoundingMode.HALF_UP),
                r.isGain() ? "GAIN" : "LOSS"
        );
    }
}
