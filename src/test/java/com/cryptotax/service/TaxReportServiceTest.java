package com.cryptotax.service;

import com.cryptotax.dto.TaxReportDto;
import com.cryptotax.model.Transaction;
import com.cryptotax.model.TransactionType;
import com.cryptotax.service.impl.FifoCalculatorService;
import com.cryptotax.service.impl.TaxReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxReportServiceTest {

    private TaxReportService taxReportService;

    @BeforeEach
    void setUp() {
        taxReportService = new TaxReportService(new FifoCalculatorService());
    }

    @Test
    void aggregatesTotalsCorrectly() {
        LocalDateTime t1 = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2024, 3, 1, 0, 0);
        LocalDateTime t4 = LocalDateTime.of(2024, 4, 1, 0, 0);

        // BTC: buy 1@30k, sell 1@40k → +10k gain
        // ETH: buy 1@2000, sell 1@1500 → -500 loss
        List<Transaction> txs = List.of(
                new Transaction(t1, TransactionType.BUY, "BTC",
                        new BigDecimal("1"), new BigDecimal("30000"), BigDecimal.ZERO),
                new Transaction(t1, TransactionType.BUY, "ETH",
                        new BigDecimal("1"), new BigDecimal("2000"), BigDecimal.ZERO),
                new Transaction(t2, TransactionType.SELL, "BTC",
                        new BigDecimal("1"), new BigDecimal("40000"), BigDecimal.ZERO),
                new Transaction(t3, TransactionType.SELL, "ETH",
                        new BigDecimal("1"), new BigDecimal("1500"), BigDecimal.ZERO),
                new Transaction(t4, TransactionType.BUY, "SOL",
                        new BigDecimal("10"), new BigDecimal("100"), BigDecimal.ZERO)
        );

        TaxReportDto report = taxReportService.buildReport(txs);

        assertThat(report.totalRealizedGains()).isEqualByComparingTo("10000.00");
        assertThat(report.totalRealizedLosses()).isEqualByComparingTo("-500.00");
        assertThat(report.netPosition()).isEqualByComparingTo("9500.00");
        assertThat(report.trades()).hasSize(2);

        // SOL position still open
        assertThat(report.remainingPositions()).containsKey("SOL");
        assertThat(report.remainingPositions().get("SOL")).isEqualByComparingTo("10");
    }
}
