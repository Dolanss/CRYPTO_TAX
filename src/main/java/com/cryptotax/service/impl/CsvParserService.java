package com.cryptotax.service.impl;

import com.cryptotax.exception.CsvParseException;
import com.cryptotax.model.Transaction;
import com.cryptotax.model.TransactionType;
import com.cryptotax.service.strategy.AssetStrategyRegistry;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AssetStrategyRegistry assetRegistry;

    public CsvParserService(AssetStrategyRegistry assetRegistry) {
        this.assetRegistry = assetRegistry;
    }

    public List<Transaction> parse(MultipartFile file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new CsvParseException("CSV file is empty");
            }

            // Skip header row
            List<Transaction> transactions = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (isBlankRow(row)) continue;
                transactions.add(parseRow(row, i + 1));
            }

            if (transactions.isEmpty()) {
                throw new CsvParseException("CSV contains no transactions");
            }

            return transactions;
        } catch (CsvParseException e) {
            throw e;
        } catch (CsvException e) {
            throw new CsvParseException("Malformed CSV: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CsvParseException("Failed to read CSV: " + e.getMessage(), e);
        }
    }

    private Transaction parseRow(String[] row, int lineNumber) {
        if (row.length < 6) {
            throw new CsvParseException(
                    "Line " + lineNumber + ": expected 6 columns but found " + row.length);
        }
        try {
            LocalDateTime date = LocalDateTime.parse(row[0].trim(), DATE_FORMAT);
            TransactionType type = TransactionType.valueOf(row[1].trim().toUpperCase());
            String asset = row[2].trim().toUpperCase();
            BigDecimal quantity = new BigDecimal(row[3].trim());
            BigDecimal priceUsd = new BigDecimal(row[4].trim());
            BigDecimal feeUsd = new BigDecimal(row[5].trim());

            assetRegistry.getStrategy(asset); // validate asset is supported
            assetRegistry.getStrategy(asset).validate(quantity);

            return new Transaction(date, type, asset, quantity, priceUsd, feeUsd);
        } catch (DateTimeParseException e) {
            throw new CsvParseException(
                    "Line " + lineNumber + ": invalid date format, expected yyyy-MM-dd HH:mm:ss");
        } catch (IllegalArgumentException e) {
            throw new CsvParseException("Line " + lineNumber + ": " + e.getMessage());
        }
    }

    private boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) return false;
        }
        return true;
    }
}
