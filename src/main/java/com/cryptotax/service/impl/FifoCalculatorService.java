package com.cryptotax.service.impl;

import com.cryptotax.exception.InsufficientLotsException;
import com.cryptotax.model.TaxLot;
import com.cryptotax.model.TradeResult;
import com.cryptotax.model.Transaction;
import com.cryptotax.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

@Service
public class FifoCalculatorService {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int SCALE = 10;

    /**
     * Processes transactions in chronological order, applying FIFO lot matching for sells.
     * Returns the realized trade results; open lots are accessible via the provided lots map.
     */
    public List<TradeResult> calculate(
            List<Transaction> transactions,
            Map<String, Deque<TaxLot>> openLots) {

        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::date))
                .toList();

        List<TradeResult> results = new ArrayList<>();

        for (Transaction tx : sorted) {
            if (tx.type() == TransactionType.BUY) {
                processBuy(tx, openLots);
            } else {
                results.add(processSell(tx, openLots));
            }
        }

        return results;
    }

    private void processBuy(Transaction tx, Map<String, Deque<TaxLot>> openLots) {
        // Cost basis per unit includes proportional fee
        BigDecimal totalCost = tx.priceUsd().multiply(tx.quantity(), MC).add(tx.feeUsd());
        BigDecimal costPerUnit = totalCost.divide(tx.quantity(), SCALE, RoundingMode.HALF_UP);

        TaxLot lot = new TaxLot(tx.quantity(), costPerUnit, tx.date());
        openLots.computeIfAbsent(tx.asset(), k -> new ArrayDeque<>()).addLast(lot);
    }

    private TradeResult processSell(Transaction tx, Map<String, Deque<TaxLot>> openLots) {
        Deque<TaxLot> lots = openLots.getOrDefault(tx.asset(), new ArrayDeque<>());
        BigDecimal remainingToSell = tx.quantity();
        BigDecimal totalCostBasis = BigDecimal.ZERO;

        // FIFO: consume oldest lots first
        while (remainingToSell.compareTo(BigDecimal.ZERO) > 0) {
            if (lots.isEmpty()) {
                throw new InsufficientLotsException(
                        "Cannot sell " + tx.quantity() + " " + tx.asset() +
                        " on " + tx.date() + ": insufficient open lots");
            }

            TaxLot lot = lots.peekFirst();
            BigDecimal consumed = remainingToSell.min(lot.getRemainingQuantity());

            totalCostBasis = totalCostBasis.add(
                    consumed.multiply(lot.getCostBasisPerUnit(), MC));

            lot.deduct(consumed);
            remainingToSell = remainingToSell.subtract(consumed);

            if (lot.isExhausted()) {
                lots.pollFirst();
            }
        }

        // Proceeds minus sell-side fees
        BigDecimal proceeds = tx.priceUsd().multiply(tx.quantity(), MC).subtract(tx.feeUsd());
        BigDecimal gainLoss = proceeds.subtract(totalCostBasis)
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new TradeResult(tx.date(), tx.asset(), tx.quantity(), proceeds,
                totalCostBasis.setScale(SCALE, RoundingMode.HALF_UP), gainLoss);
    }
}
