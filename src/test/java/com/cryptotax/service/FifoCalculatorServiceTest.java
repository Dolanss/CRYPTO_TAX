package com.cryptotax.service;

import com.cryptotax.exception.InsufficientLotsException;
import com.cryptotax.model.*;
import com.cryptotax.service.impl.FifoCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class FifoCalculatorServiceTest {

    private FifoCalculatorService calculator;

    private static final LocalDateTime T1 = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2024, 2, 1, 0, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2024, 3, 1, 0, 0);
    private static final LocalDateTime T4 = LocalDateTime.of(2024, 4, 1, 0, 0);

    @BeforeEach
    void setUp() {
        calculator = new FifoCalculatorService();
    }

    private Transaction buy(LocalDateTime date, String asset, String qty, String price) {
        return new Transaction(date, TransactionType.BUY, asset,
                new BigDecimal(qty), new BigDecimal(price), BigDecimal.ZERO);
    }

    private Transaction sell(LocalDateTime date, String asset, String qty, String price) {
        return new Transaction(date, TransactionType.SELL, asset,
                new BigDecimal(qty), new BigDecimal(price), BigDecimal.ZERO);
    }

    private Transaction sellWithFee(LocalDateTime date, String asset, String qty,
                                    String price, String fee) {
        return new Transaction(date, TransactionType.SELL, asset,
                new BigDecimal(qty), new BigDecimal(price), new BigDecimal(fee));
    }

    @Nested
    @DisplayName("Simple gain/loss scenarios")
    class SimpleScenarios {

        @Test
        @DisplayName("Single buy then full sell — gain")
        void singleBuySell_gain() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "30000"),
                    sell(T2, "BTC", "1", "40000")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            assertThat(results).hasSize(1);
            TradeResult r = results.get(0);
            assertThat(r.gainLoss()).isEqualByComparingTo("10000");
            assertThat(r.isGain()).isTrue();
        }

        @Test
        @DisplayName("Single buy then full sell — loss")
        void singleBuySell_loss() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "50000"),
                    sell(T2, "BTC", "1", "40000")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("-10000");
            assertThat(results.get(0).isGain()).isFalse();
        }

        @Test
        @DisplayName("Fees are included in cost basis")
        void feeIncludedInCostBasis() {
            var txs = List.of(
                    new Transaction(T1, TransactionType.BUY, "BTC",
                            new BigDecimal("1"), new BigDecimal("30000"), new BigDecimal("100")),
                    sell(T2, "BTC", "1", "30000")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            // Cost basis = 30000 + 100 fee = 30100; proceeds = 30000; gain = -100
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("-100");
        }

        @Test
        @DisplayName("Sell fees reduce proceeds")
        void sellFeeReducesProceeds() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "30000"),
                    sellWithFee(T2, "BTC", "1", "40000", "50")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            // Proceeds = 40000 - 50 = 39950; cost = 30000; gain = 9950
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("9950");
        }
    }

    @Nested
    @DisplayName("FIFO lot matching")
    class FifoLotMatching {

        @Test
        @DisplayName("Two lots — oldest matched first")
        void twoLots_oldestFirst() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "20000"),  // lot 1: 1 BTC @ 20k
                    buy(T2, "BTC", "1", "40000"),  // lot 2: 1 BTC @ 40k
                    sell(T3, "BTC", "1", "50000")  // should match lot 1
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            // gain = 50000 - 20000 = 30000 (used oldest lot)
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("30000");

            // lot 2 still open with 1 BTC
            assertThat(lots.get("BTC")).hasSize(1);
            assertThat(lots.get("BTC").peekFirst().getRemainingQuantity())
                    .isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("Partial fill — lot partially consumed")
        void partialFill_lotPartiallyConsumed() {
            var txs = List.of(
                    buy(T1, "BTC", "2", "30000"),  // lot: 2 BTC @ 30k each
                    sell(T2, "BTC", "0.5", "40000") // sell 0.5 BTC
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            // proceeds = 0.5 * 40000 = 20000; cost = 0.5 * 30000 = 15000; gain = 5000
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("5000");

            // 1.5 BTC remain in the lot
            assertThat(lots.get("BTC").peekFirst().getRemainingQuantity())
                    .isEqualByComparingTo("1.5");
        }

        @Test
        @DisplayName("Sell spans multiple lots")
        void sellSpansMultipleLots() {
            var txs = List.of(
                    buy(T1, "ETH", "2", "1000"),   // lot 1: 2 ETH @ 1000
                    buy(T2, "ETH", "3", "2000"),   // lot 2: 3 ETH @ 2000
                    sell(T3, "ETH", "4", "3000")   // sells 2 from lot1 + 2 from lot2
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            // cost = 2*1000 + 2*2000 = 6000; proceeds = 4*3000 = 12000; gain = 6000
            assertThat(results.get(0).costBasis()).isEqualByComparingTo("6000");
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("6000");

            // 1 ETH remains in lot 2
            assertThat(lots.get("ETH")).hasSize(1);
            assertThat(lots.get("ETH").peekFirst().getRemainingQuantity())
                    .isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("Exact lot exhaustion — lot removed from queue")
        void exactLotExhaustion() {
            var txs = List.of(
                    buy(T1, "SOL", "10", "100"),
                    sell(T2, "SOL", "10", "150")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            calculator.calculate(txs, lots);

            assertThat(lots.get("SOL")).isEmpty();
        }

        @Test
        @DisplayName("Multiple sells across lots — running FIFO state")
        void multipleSells_runningFifoState() {
            var txs = List.of(
                    buy(T1, "BTC", "3", "10000"),
                    sell(T2, "BTC", "1", "15000"),
                    sell(T3, "BTC", "1", "20000"),
                    sell(T4, "BTC", "1", "25000")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).gainLoss()).isEqualByComparingTo("5000");
            assertThat(results.get(1).gainLoss()).isEqualByComparingTo("10000");
            assertThat(results.get(2).gainLoss()).isEqualByComparingTo("15000");
        }
    }

    @Nested
    @DisplayName("Multiple assets — isolated lot queues")
    class MultipleAssets {

        @Test
        @DisplayName("BTC and ETH lots do not interfere")
        void btcAndEthIsolated() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "30000"),
                    buy(T1, "ETH", "10", "2000"),
                    sell(T2, "BTC", "1", "35000"),
                    sell(T2, "ETH", "10", "2500")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();
            List<TradeResult> results = calculator.calculate(txs, lots);

            assertThat(results).hasSize(2);
            TradeResult btcResult = results.stream()
                    .filter(r -> r.asset().equals("BTC")).findFirst().orElseThrow();
            TradeResult ethResult = results.stream()
                    .filter(r -> r.asset().equals("ETH")).findFirst().orElseThrow();

            assertThat(btcResult.gainLoss()).isEqualByComparingTo("5000");
            assertThat(ethResult.gainLoss()).isEqualByComparingTo("5000");
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("Sell without prior buy throws InsufficientLotsException")
        void sellWithoutBuy_throws() {
            var txs = List.of(sell(T1, "BTC", "1", "50000"));
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();

            assertThatThrownBy(() -> calculator.calculate(txs, lots))
                    .isInstanceOf(InsufficientLotsException.class)
                    .hasMessageContaining("insufficient open lots");
        }

        @Test
        @DisplayName("Sell more than available throws InsufficientLotsException")
        void sellMoreThanAvailable_throws() {
            var txs = List.of(
                    buy(T1, "BTC", "1", "30000"),
                    sell(T2, "BTC", "2", "40000")
            );
            Map<String, Deque<TaxLot>> lots = new LinkedHashMap<>();

            assertThatThrownBy(() -> calculator.calculate(txs, lots))
                    .isInstanceOf(InsufficientLotsException.class);
        }
    }
}
