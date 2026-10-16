package com.oskarro.oskarito.stats;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    SiteStats stats() {
        return statsService.stats();
    }

    @GetMapping("/added")
    List<DailyAdds> added() {
        return statsService.communityAdds();
    }

    @PostMapping("/visit")
    SiteStats visit(@RequestBody VisitNote note) {
        statsService.noteVisit(note == null ? null : note.visitor());
        return statsService.stats();
    }

    public record VisitNote(String visitor) {
    }
}
