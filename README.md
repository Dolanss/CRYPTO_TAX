# Crypto Tax Reporter

Open-source crypto capital gains calculator using **FIFO cost basis**. Built with Java 17 + Spring Boot 3. Inspired by the architecture of production tax-engine systems.

## Features

- **FIFO lot matching** — oldest buys are matched against sells first
- **Per-trade gain/loss** — proceeds, cost basis, and net for every sell
- **Summary** — total realized gains, total realized losses, net position
- **Open positions** — remaining quantity per asset after all sells
- **Async processing** — large files run in a background thread pool; poll by jobId
- **Strategy pattern** — one class per asset (BTC, ETH, SOL), trivially extensible
- **REST API** — synchronous and async endpoints
- **Swagger UI** at `/swagger-ui.html`

---

## Quick Start

### With Docker Compose

```bash
docker compose up --build
```

API available at `http://localhost:8080`.

### From source

```bash
mvn spring-boot:run
```

---

## CSV Format

| Column      | Type   | Example                 | Notes                        |
|-------------|--------|-------------------------|------------------------------|
| `date`      | string | `2024-01-15 10:00:00`   | `yyyy-MM-dd HH:mm:ss`        |
| `type`      | string | `BUY` / `SELL`          | Case-insensitive             |
| `asset`     | string | `BTC`, `ETH`, `SOL`     | Must be a supported asset    |
| `quantity`  | number | `0.5`                   | Positive, any precision      |
| `price_usd` | number | `42000.00`              | Per-unit USD price           |
| `fee_usd`   | number | `21.00`                 | Total fee for the trade      |

**Example CSV (`sample_transactions.csv`):**

```csv
date,type,asset,quantity,price_usd,fee_usd
2024-01-15 10:00:00,BUY,BTC,0.5,42000.00,21.00
2024-01-20 14:30:00,BUY,ETH,5.0,2500.00,12.50
2024-02-01 09:00:00,BUY,BTC,0.3,45000.00,13.50
2024-02-10 11:00:00,BUY,SOL,100.0,95.00,9.50
2024-03-05 16:00:00,SELL,BTC,0.4,55000.00,22.00
2024-03-20 10:30:00,SELL,ETH,2.0,3200.00,6.40
2024-04-01 08:00:00,BUY,BTC,0.2,60000.00,12.00
2024-04-15 15:00:00,SELL,SOL,50.0,150.00,7.50
2024-05-01 12:00:00,SELL,BTC,0.6,65000.00,39.00
```

---

## API Reference

### `POST /api/v1/calculate` — Synchronous (small files)

```bash
curl -X POST http://localhost:8080/api/v1/calculate \
  -F "file=@sample_transactions.csv"
```

**Response:**

```json
{
  "trades": [
    {
      "date": "2024-03-05T16:00:00",
      "asset": "BTC",
      "quantitySold": 0.40000000,
      "proceeds": 21978.00,
      "costBasis": 16804.00,
      "gainLoss": 5174.00,
      "type": "GAIN"
    }
  ],
  "totalRealizedGains": "14624.00",
  "totalRealizedLosses": "0.00",
  "netPosition": "14624.00",
  "remainingPositions": {
    "ETH": "3.00000000",
    "SOL": "50.00000000"
  }
}
```

### `POST /api/v1/calculate/async` — Async (large files)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/async \
  -F "file=@sample_transactions.csv"
# → { "jobId": "b3f2a1d0-..." }
```

### `GET /api/v1/calculate/{jobId}` — Poll job status

```bash
curl http://localhost:8080/api/v1/calculate/b3f2a1d0-...
```

**Response while processing:**
```json
{ "jobId": "...", "status": "PROCESSING", "result": null, "errorMessage": null }
```

**Response when complete:**
```json
{ "jobId": "...", "status": "COMPLETED", "result": { ...full report... }, "errorMessage": null }
```

---

## FIFO Cost Basis Logic

1. Buys create **tax lots** `(quantity, cost-per-unit, date)` stored in a FIFO queue per asset.
2. For each sell, the engine dequeues the **oldest lots first**, consuming until the sold quantity is matched.
3. **Buy cost basis** = `(price × quantity + fee) / quantity` (fee is amortized into each unit).
4. **Sell proceeds** = `price × quantity − fee`.
5. **Gain/loss** = `proceeds − Σ(consumed_quantity × cost_basis_per_unit_of_lot)`.

---

## Adding a New Asset

Create a Spring `@Component` implementing `AssetStrategy`:

```java
@Component
public class MaticStrategy implements AssetStrategy {
    @Override public String getAssetSymbol() { return "MATIC"; }
    @Override public int getPrecision()       { return 8; }
}
```

`AssetStrategyRegistry` auto-discovers it — no other changes needed.

---

## Running Tests

```bash
mvn test
```

Test coverage includes:
- Single buy/sell gain and loss
- Fee included in cost basis (buy side)
- Fee reduces proceeds (sell side)
- Partial lot consumption
- Sell spanning multiple lots
- Multiple sequential sells from one lot
- BTC/ETH isolation (independent lot queues)
- Sell without buy → `InsufficientLotsException`
- Sell more than available → `InsufficientLotsException`
- Controller layer (MockMvc, mocked orchestrator)

---

## Architecture

```
controller/
  TaxCalculatorController     REST endpoints (sync + async + status poll)
service/
  impl/
    CsvParserService          CSV → List<Transaction> with validation
    FifoCalculatorService     FIFO lot matching, returns List<TradeResult>
    TaxReportService          Aggregates results into TaxReportDto
    CalculationOrchestrator   Wires CSV → FIFO → report, manages job lifecycle
  strategy/
    AssetStrategy             Interface (getAssetSymbol, getPrecision, validate)
    BtcStrategy / EthStrategy / SolStrategy
    AssetStrategyRegistry     Spring-injected map of all strategies
async/
  AsyncCalculationTask        @Async wrapper for background jobs
repository/
  JobRepository               In-memory ConcurrentHashMap job store
model/
  Transaction, TaxLot, TradeResult, CalculationJob, enums
dto/
  TaxReportDto, TradeResultDto, JobStatusDto
```
