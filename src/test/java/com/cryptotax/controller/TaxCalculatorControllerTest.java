package com.cryptotax.controller;

import com.cryptotax.dto.TaxReportDto;
import com.cryptotax.dto.TradeResultDto;
import com.cryptotax.service.impl.CalculationOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaxCalculatorController.class)
class TaxCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculationOrchestrator orchestrator;

    @Test
    void calculateEndpoint_returns200WithReport() throws Exception {
        TaxReportDto mockReport = new TaxReportDto(
                List.of(new TradeResultDto(
                        LocalDateTime.of(2024, 2, 1, 0, 0),
                        "BTC",
                        new BigDecimal("1"),
                        new BigDecimal("40000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("10000.00"),
                        "GAIN"
                )),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000.00"),
                Map.of()
        );

        when(orchestrator.calculateSync(any())).thenReturn(mockReport);

        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv",
                "text/csv", "header\n".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/calculate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netPosition").value(10000.00))
                .andExpect(jsonPath("$.totalRealizedGains").value(10000.00))
                .andExpect(jsonPath("$.trades[0].asset").value("BTC"))
                .andExpect(jsonPath("$.trades[0].type").value("GAIN"));
    }

    @Test
    void asyncEndpoint_returns202WithJobId() throws Exception {
        when(orchestrator.submitAsync(any())).thenReturn("test-job-123");

        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv",
                "text/csv", "header\n".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/calculate/async").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("test-job-123"));
    }
}
