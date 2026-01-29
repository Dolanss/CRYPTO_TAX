package com.cryptotax.controller;

import com.cryptotax.dto.JobStatusDto;
import com.cryptotax.dto.TaxReportDto;
import com.cryptotax.service.impl.CalculationOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/calculate")
@Tag(name = "Tax Calculator", description = "Crypto capital gains calculation endpoints")
public class TaxCalculatorController {

    private final CalculationOrchestrator orchestrator;

    public TaxCalculatorController(CalculationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Operation(
        summary = "Calculate capital gains from CSV (synchronous)",
        description = "Upload a transaction CSV and receive the full tax report immediately. " +
                      "For large files use the async endpoint.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tax report generated",
                content = @Content(schema = @Schema(implementation = TaxReportDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid CSV or unsupported asset"),
            @ApiResponse(responseCode = "422", description = "Insufficient lots for a sell")
        }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaxReportDto> calculate(
            @Parameter(description = "CSV file with columns: date, type, asset, quantity, price_usd, fee_usd")
            @RequestParam("file") MultipartFile file) {

        TaxReportDto report = orchestrator.calculateSync(file);
        return ResponseEntity.ok(report);
    }

    @Operation(
        summary = "Submit CSV for async processing",
        description = "Submits a large CSV for background processing. Returns a jobId. " +
                      "Poll GET /api/v1/calculate/{jobId} for results.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Job accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV")
        }
    )
    @PostMapping(path = "/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitAsync(
            @RequestParam("file") MultipartFile file) {

        String jobId = orchestrator.submitAsync(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("jobId", jobId));
    }

    @Operation(
        summary = "Poll async job status",
        description = "Returns PENDING, PROCESSING, COMPLETED (with report), or FAILED (with error).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Job status returned"),
            @ApiResponse(responseCode = "404", description = "Job not found")
        }
    )
    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusDto> getJobStatus(
            @PathVariable String jobId) {

        return ResponseEntity.ok(orchestrator.getJobStatus(jobId));
    }
}
