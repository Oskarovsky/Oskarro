package com.oskarro.oskarito.report;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Public "Zgłoś naruszenie" endpoint; the admin side lives in AdminController. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @PostMapping
    Map<String, Object> submit(@RequestBody ReportService.NewReport report) {
        reports.submit(report);
        return Map.of("ok", true);
    }
}
