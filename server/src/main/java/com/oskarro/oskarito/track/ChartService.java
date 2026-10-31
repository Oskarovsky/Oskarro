package com.oskarro.oskarito.track;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Hit charts on the ranking page: one chart per genre (singles) or for sets, never mixed.
 * Points in a period = 5 × net votes cast in it + plays in it; all-time counts everything.
 */
@Service
public class ChartService {

    static final int VOTE_WEIGHT = 5;
    /** Always ten rows: ties at the end are cut, the order between them is votes, then newest. */
    private static final int SIZE = 10;

    @PersistenceContext
    private EntityManager em;

    private final TrackSocialService social;

    public ChartService(TrackSocialService social) {
        this.social = social;
    }

    public record ChartRow(int place, int points, long votes, long plays, Track track) {
    }

    public record Chart(String period, String category, List<ChartRow> rows) {
    }

    @Transactional(readOnly = true)
    public Chart chart(String period, String category) {
        Instant since = switch (period == null ? "week" : period) {
            case "week" -> Instant.now().minus(Duration.ofDays(7));
            case "month" -> Instant.now().minus(Duration.ofDays(30));
            case "all" -> null;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Okres: week, month albo all");
        };
        boolean sets = "sety".equalsIgnoreCase(category) || "sets".equalsIgnoreCase(category);
        String genre = sets ? null : genreOf(category);

        String where = "t.url is not null and t.url <> '' and t.genre is not null and t.genre <> ''"
                + (sets ? " and t.kind = 'set'" : " and (t.kind is null or t.kind <> 'set') and upper(t.genre) = :genre");
        var candidates = em.createQuery("select t.id from Track t where " + where, Integer.class);
        if (!sets) {
            candidates.setParameter("genre", genre);
        }
        Set<Integer> ids = new HashSet<>(candidates.getResultList());
        if (ids.isEmpty()) {
            return new Chart(period, category, List.of());
        }

        Map<Integer, Long> votes = grouped("select l.track.id, sum(l.vote) from TrackLike l where l.track.id in :ids"
                + (since == null ? "" : " and l.createdAt >= :since") + " group by l.track.id", ids, since);
        Map<Integer, Long> plays = grouped("select v.trackId, count(v) from TrackView v where v.trackId in :ids"
                + (since == null ? "" : " and v.playedAt >= :since") + " group by v.trackId", ids, since);

        record Entry(Integer id, long votes, long plays, long points) {
        }
        List<Entry> ranked = ids.stream()
                .map(id -> {
                    long v = votes.getOrDefault(id, 0L);
                    long p = plays.getOrDefault(id, 0L);
                    return new Entry(id, v, p, VOTE_WEIGHT * v + p);
                })
                // A weekly or monthly chart lists only what was played or voted on in that time.
                .filter(e -> since == null || e.votes() != 0 || e.plays() != 0)
                .sorted(Comparator.comparingLong(Entry::points).reversed()
                        .thenComparing(Comparator.comparingLong(Entry::votes).reversed())
                        .thenComparing(Comparator.comparing(Entry::id).reversed()))
                .limit(SIZE)
                .toList();
        if (ranked.isEmpty()) {
            return new Chart(period, category, List.of());
        }

        Map<Integer, Track> byId = new HashMap<>();
        em.createQuery("select t from Track t where t.id in :ids", Track.class)
                .setParameter("ids", ranked.stream().map(Entry::id).toList())
                .getResultList().forEach(t -> byId.put(t.getId(), t));
        List<Track> tracks = ranked.stream().map(e -> byId.get(e.id())).filter(Objects::nonNull).toList();
        social.decorateAll(new ArrayList<>(tracks));

        List<ChartRow> rows = new ArrayList<>();
        int place = 0;
        long previous = Long.MIN_VALUE;
        int index = 0;
        for (Entry e : ranked) {
            index++;
            Track track = byId.get(e.id());
            if (track == null) {
                continue;
            }
            // Equal points share a place (1, 2, 2, 4).
            if (e.points() != previous) {
                place = index;
                previous = e.points();
            }
            rows.add(new ChartRow(place, (int) e.points(), e.votes(), e.plays(), track));
        }
        return new Chart(period, sets ? "sety" : category.toLowerCase(Locale.ROOT), rows);
    }

    private Map<Integer, Long> grouped(String query, Set<Integer> ids, Instant since) {
        var q = em.createQuery(query, Object[].class).setParameter("ids", ids);
        if (since != null) {
            q.setParameter("since", since);
        }
        Map<Integer, Long> out = new HashMap<>();
        for (Object[] row : q.getResultList()) {
            out.put(((Number) row[0]).intValue(), row[1] == null ? 0L : ((Number) row[1]).longValue());
        }
        return out;
    }

    private static String genreOf(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz gatunek albo sety");
        }
        String key = category.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            Genre.valueOf(key);
        } catch (IllegalArgumentException unknown) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany gatunek");
        }
        return key;
    }
}
