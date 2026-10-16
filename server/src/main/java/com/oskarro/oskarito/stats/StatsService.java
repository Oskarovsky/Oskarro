package com.oskarro.oskarito.stats;

import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackViewRepository;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class StatsService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final int ADDED_DAYS = 14;
    private static final Pattern VISITOR = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final SiteVisitRepository visits;
    private final UserRepository users;
    private final TrackRepository tracks;
    private final TrackViewRepository plays;

    public StatsService(SiteVisitRepository visits,
                        UserRepository users,
                        TrackRepository tracks,
                        TrackViewRepository plays) {
        this.visits = visits;
        this.users = users;
        this.tracks = tracks;
        this.plays = plays;
    }

    public void noteVisit(String visitor) {
        if (visitor == null || !VISITOR.matcher(visitor).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznane wejście");
        }
        LocalDate today = LocalDate.now(WARSAW);
        if (visits.existsByVisitorAndDay(visitor, today)) {
            return;
        }
        try {
            visits.save(new SiteVisit(visitor, today));
        } catch (DataIntegrityViolationException ignored) {
            // Drugie wejście z tej samej przeglądarki w tym dniu.
        }
    }

    public SiteStats stats() {
        LocalDate today = LocalDate.now(WARSAW);
        return new SiteStats(
                visits.countByDay(today),
                users.count(),
                tracks.countCommunity(),
                plays.count());
    }

    public List<DailyAdds> communityAdds() {
        LocalDate today = LocalDate.now(WARSAW);
        LocalDate start = today.minusDays(ADDED_DAYS - 1L);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Instant created : tracks.findCommunityCreatedSince(start.atStartOfDay(WARSAW).toInstant())) {
            if (created == null) {
                continue;
            }
            LocalDate day = created.atZone(WARSAW).toLocalDate();
            if (day.isBefore(start) || day.isAfter(today)) {
                continue;
            }
            counts.merge(day, 1L, Long::sum);
        }
        List<DailyAdds> days = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            days.add(new DailyAdds(day.toString(), counts.getOrDefault(day, 0L)));
        }
        return days;
    }
}
