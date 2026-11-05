package com.clinic.controller;

import com.clinic.dto.response.OverviewResponse;
import com.clinic.dto.response.TrendResponse;
import com.clinic.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Dashboard numbers and appointment statistics")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard cards - Admin only")
    public ResponseEntity<OverviewResponse> overview() {
        return ResponseEntity.ok(reportService.overview());
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Appointment counts per day, week or month - Admin only")
    public ResponseEntity<TrendResponse> appointments(
            @RequestParam(defaultValue = "DAILY") String period) {
        return ResponseEntity.ok(reportService.appointmentTrend(period));
    }
}
