package com.oskarro.oskarito.track;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public hit charts for the ranking page. */
@RestController
public class ChartController {

    private final ChartService charts;

    public ChartController(ChartService charts) {
        this.charts = charts;
    }

    /** period: week | month | all; category: a genre slug (club, disco-polo, ...) or "sety". */
    @GetMapping("/api/charts")
    ChartService.Chart chart(@RequestParam(defaultValue = "week") String period,
                             @RequestParam(defaultValue = "club") String category) {
        return charts.chart(period, category);
    }
}
