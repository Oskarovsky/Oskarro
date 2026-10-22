package com.oskarro.oskarito.track;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Read paths that must not load the whole catalogue: the home page feed and search.
 * Only the tracks actually shown are decorated (votes, plays, covers).
 */
@Service
@Transactional(readOnly = true)
public class TrackQueryService {

    private static final String COMMUNITY = "t.genre is not null and t.genre <> '' and t.url is not null and t.url <> ''";
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @PersistenceContext
    private EntityManager em;

    private final TrackSocialService social;

    public TrackQueryService(TrackSocialService social) {
        this.social = social;
    }

    public record HomeFeed(List<Track> latest, List<Track> hot, Map<String, Long> genreCounts, Map<String, Long> setCounts) {
    }

    public HomeFeed home() {
        List<Track> latest = em.createQuery("select t from Track t where " + COMMUNITY + " order by t.id desc", Track.class)
                .setMaxResults(6).getResultList();

        Instant monthStart = LocalDate.now(WARSAW).withDayOfMonth(1).atStartOfDay(WARSAW).toInstant();
        List<Integer> hotIds = em.createQuery(
                        "select v.trackId from TrackView v where v.playedAt >= :since group by v.trackId order by count(v) desc", Integer.class)
                .setParameter("since", monthStart).setMaxResults(10).getResultList();
        List<Track> hot = new ArrayList<>();
        if (!hotIds.isEmpty()) {
            Map<Integer, Track> byId = new HashMap<>();
            em.createQuery("select t from Track t where t.id in :ids and " + COMMUNITY, Track.class)
                    .setParameter("ids", hotIds).getResultList().forEach(track -> byId.put(track.getId(), track));
            hotIds.stream().map(byId::get).filter(Objects::nonNull).limit(3).forEach(hot::add);
        }
        for (Track track : latest) {
            if (hot.size() >= 3) {
                break;
            }
            if (hot.stream().noneMatch(chosen -> chosen.getId().equals(track.getId()))) {
                hot.add(track);
            }
        }

        Map<String, Long> genreCounts = new LinkedHashMap<>();
        for (Object[] row : em.createQuery("select t.genre, count(t) from Track t where " + COMMUNITY
                + " and (t.kind is null or t.kind <> 'set') group by t.genre", Object[].class).getResultList()) {
            genreCounts.put((String) row[0], ((Number) row[1]).longValue());
        }
        Map<String, Long> setCounts = new LinkedHashMap<>();
        for (Object[] row : em.createQuery("select t.setCategory, count(t) from Track t where t.kind = 'set' group by t.setCategory",
                Object[].class).getResultList()) {
            setCounts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<Track> shown = new ArrayList<>(latest);
        hot.stream().filter(track -> !shown.contains(track)).forEach(shown::add);
        social.decorateAll(shown);
        return new HomeFeed(latest, hot, genreCounts, setCounts);
    }

    /** Every word must appear in title, artist, version, event, tracklist, genre or the sharer's name. */
    public List<Track> search(String query, int limit) {
        List<String> words = Arrays.stream((query == null ? "" : query).toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .filter(word -> !word.isBlank()).limit(6).toList();
        if (words.isEmpty()) {
            return List.of();
        }
        String haystack = "lower(concat(coalesce(t.title,''),' ',coalesce(t.artist,''),' ',coalesce(t.version,''),' ',"
                + "coalesce(t.eventName,''),' ',coalesce(t.tracklist,''),' ',coalesce(t.genre,''),' ',coalesce(a.username,'')))";
        StringBuilder jpql = new StringBuilder("select t from Track t left join t.author a where " + COMMUNITY);
        for (int i = 0; i < words.size(); i++) {
            jpql.append(" and ").append(haystack).append(" like :w").append(i).append(" escape '!'");
        }
        jpql.append(" order by t.id desc");
        TypedQuery<Track> q = em.createQuery(jpql.toString(), Track.class).setMaxResults(Math.max(1, Math.min(limit, 200)));
        for (int i = 0; i < words.size(); i++) {
            q.setParameter("w" + i, "%" + words.get(i).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        return social.decorateAll(new ArrayList<>(q.getResultList()));
    }

    private static final Set<String> NOISE = Set.of("feat", "ft", "featuring", "x", "vs", "and", "the", "i", "oraz", "remix", "mix",
            "edit", "extended", "original", "radio", "version", "bootleg");

    /**
     * Up to {@code limit} existing tracks that look like the one being typed in: the same file link, or many of the
     * same words in artist and title (case, Polish letters, "feat." and word order do not matter).
     */
    public List<SimilarTrack> similar(String artist, String title, String url, Integer exceptId, int limit) {
        Set<String> wantedTitle = words(title);
        Set<String> wantedArtist = words(artist);
        String wantedFile = TrackSocialService.canonical(url);
        if (wantedTitle.isEmpty() && wantedArtist.isEmpty() && wantedFile == null) {
            return List.of();
        }
        record Hit(int id, boolean sameFile, double score) {}
        List<Hit> hits = new ArrayList<>();
        for (Object[] row : em.createQuery("select t.id, t.artist, t.title, t.url from Track t where " + COMMUNITY, Object[].class)
                .getResultList()) {
            int id = ((Number) row[0]).intValue();
            if (exceptId != null && exceptId == id) {
                continue;
            }
            boolean sameFile = wantedFile != null && wantedFile.equals(TrackSocialService.canonical((String) row[3]));
            double titleScore = overlap(wantedTitle, words((String) row[2]));
            double artistScore = overlap(wantedArtist, words((String) row[1]));
            double score = sameFile ? 1.0
                    : wantedArtist.isEmpty() ? titleScore
                    : wantedTitle.isEmpty() ? artistScore * 0.6
                    : titleScore * 0.65 + artistScore * 0.35;
            if (sameFile || score >= 0.45) {
                hits.add(new Hit(id, sameFile, score));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        List<Hit> top = hits.subList(0, Math.min(Math.max(1, limit), hits.size()));
        if (top.isEmpty()) {
            return List.of();
        }
        Map<Integer, Track> byId = new HashMap<>();
        List<Track> tracks = em.createQuery("select t from Track t where t.id in :ids", Track.class)
                .setParameter("ids", top.stream().map(Hit::id).toList()).getResultList();
        social.decorateAll(new ArrayList<>(tracks)).forEach(track -> byId.put(track.getId(), track));
        return top.stream()
                .filter(hit -> byId.containsKey(hit.id()))
                .map(hit -> new SimilarTrack(byId.get(hit.id()), hit.sameFile(), (int) Math.round(hit.score() * 100)))
                .toList();
    }

    static Set<String> words(String text) {
        String plain = java.text.Normalizer.normalize(text == null ? "" : text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('ł', 'l').replace('Ł', 'L').toLowerCase(Locale.ROOT);
        Set<String> out = new LinkedHashSet<>();
        for (String word : plain.split("[^a-z0-9]+")) {
            if (word.length() >= 2 && !NOISE.contains(word)) {
                out.add(word);
            }
        }
        return out;
    }

    /** Share of the shorter word set found in the other one (0..1). */
    private static double overlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long common = a.stream().filter(b::contains).count();
        return (double) common / Math.min(a.size(), b.size());
    }
}
