package com.oskarro.oskarito.explore;

import com.oskarro.oskarito.seo.SeoPageController;
import com.oskarro.oskarito.track.ChartService;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackSocialService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Landing pages people search for: "[gatunek] [miesiąc] [rok]" (monthly new tracks per genre),
 * "[wykonawca] mp3" (artist pages) and the long-form genre text. The same data feeds the Angular
 * pages and the server-rendered HTML for search engines.
 */
@Service
public class ExploreService {

    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final String[] MONTH = {"styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec", "lipiec",
            "sierpień", "wrzesień", "październik", "listopad", "grudzień"};
    /** "w listopadzie" — locative for descriptions. */
    private static final String[] MONTH_IN = {"styczniu", "lutym", "marcu", "kwietniu", "maju", "czerwcu", "lipcu",
            "sierpniu", "wrześniu", "październiku", "listopadzie", "grudniu"};
    private static final Pattern ARTIST_SPLIT = Pattern.compile(
            "(?i)\\s*(?:,|&|\\+|\\s(?:x|feat\\.?|ft\\.?|featuring|vs\\.?|pres\\.?|with)\\s)\\s*");
    private static final String PLAYABLE = "t.url is not null and t.url <> '' and t.genre is not null and t.genre <> ''";

    @PersistenceContext
    private EntityManager em;

    private final TrackSocialService social;
    private final ChartService charts;

    public ExploreService(TrackSocialService social, ChartService charts) {
        this.social = social;
        this.charts = charts;
    }

    public record Seo(String title, String description, String path, boolean index) {
    }

    public record MonthLink(String month, String label, long tracks) {
    }

    public record GenreInfo(String slug, String id, String label, List<String> intro, List<GenreContent.Faq> faq,
                            List<MonthLink> months) {
    }

    public record MonthPage(String genre, String genreLabel, String month, String label, List<Track> tracks,
                            List<ChartService.ChartRow> top, MonthLink previous, MonthLink next, Seo seo) {
    }

    public record ArtistLink(String slug, String name, long tracks) {
    }

    public record ArtistPage(String slug, String name, List<Track> tracks, Seo seo) {
    }

    // --- genres ----------------------------------------------------------------------------

    public static String[] genre(String slug) {
        return SeoPageController.GENRES.stream().filter(g -> g[1].equals(slug)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego gatunku"));
    }

    @Transactional(readOnly = true)
    public GenreInfo genreInfo(String slug) {
        String[] g = genre(slug);
        GenreContent.Content content = GenreContent.of(slug);
        return new GenreInfo(slug, g[0], g[2], content == null ? List.of(g[3]) : content.intro(),
                content == null ? List.of() : content.faq(), months(slug));
    }

    /** Months that have new tracks in a genre, newest first. */
    @Transactional(readOnly = true)
    public List<MonthLink> months(String slug) {
        String[] g = genre(slug);
        Map<YearMonth, Long> counts = new TreeMap<>(Comparator.reverseOrder());
        for (Instant created : em.createQuery("select t.createdAt from Track t where " + PLAYABLE
                        + " and (t.kind is null or t.kind <> 'set') and upper(t.genre) = :genre and t.createdAt is not null", Instant.class)
                .setParameter("genre", g[0]).getResultList()) {
            counts.merge(YearMonth.from(created.atZone(WARSAW)), 1L, Long::sum);
        }
        List<MonthLink> out = new ArrayList<>();
        counts.forEach((month, n) -> out.add(new MonthLink(month.toString(), monthLabel(month), n)));
        return out;
    }

    @Transactional(readOnly = true)
    public MonthPage month(String slug, String rawMonth) {
        String[] g = genre(slug);
        YearMonth month;
        try {
            month = YearMonth.parse(rawMonth);
        } catch (DateTimeParseException bad) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego miesiąca");
        }
        YearMonth now = YearMonth.now(WARSAW);
        if (month.isAfter(now) || month.isBefore(YearMonth.of(2000, 1))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego miesiąca");
        }
        Instant from = month.atDay(1).atStartOfDay(WARSAW).toInstant();
        Instant until = month.plusMonths(1).atDay(1).atStartOfDay(WARSAW).toInstant();
        List<Track> tracks = new ArrayList<>(em.createQuery("select t from Track t where " + PLAYABLE
                        + " and (t.kind is null or t.kind <> 'set') and upper(t.genre) = :genre"
                        + " and t.createdAt >= :from and t.createdAt < :until order by t.createdAt desc", Track.class)
                .setParameter("genre", g[0]).setParameter("from", from).setParameter("until", until).getResultList());
        if (tracks.isEmpty() && !month.equals(now)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "W tym miesiącu nie dodano utworów");
        }
        social.decorateAll(tracks);
        List<ChartService.ChartRow> top = charts.between(slug, from, until).rows();

        List<MonthLink> all = months(slug);
        MonthLink previous = all.stream().filter(m -> YearMonth.parse(m.month()).isBefore(month)).findFirst().orElse(null);
        MonthLink next = all.stream().filter(m -> YearMonth.parse(m.month()).isAfter(month))
                .reduce((first, second) -> second).orElse(null);
        String path = "/nowosci/" + slug + "/" + month;
        return new MonthPage(slug, g[2], month.toString(), monthLabel(month), tracks, top, previous, next,
                new Seo(monthTitle(g, month), monthDescription(g, month, tracks.size()), path, !tracks.isEmpty()));
    }

    public static String monthLabel(YearMonth month) {
        return MONTH[month.getMonthValue() - 1] + " " + month.getYear();
    }

    static String monthTitle(String[] g, YearMonth month) {
        String when = monthLabel(month);
        return switch (g[0]) {
            case "CLUB" -> "Muzyka klubowa " + when + " – nowości club mp3 | VIXA";
            case "VIXA" -> "Vixa " + when + " – nowości do auta i na imprezę, mp3 | VIXA";
            case "DISCO_POLO" -> "Disco polo " + when + " – nowości mp3 na wesele | VIXA";
            default -> g[2] + " " + when + " – nowości mp3 na imprezę | VIXA";
        };
    }

    static String monthDescription(String[] g, YearMonth month, int count) {
        String in = "w " + MONTH_IN[month.getMonthValue() - 1] + " " + month.getYear();
        return count == 0
                ? "Nowości " + g[2] + " " + in + " — pierwsze numery pojawią się tu wkrótce. Słuchaj z nami za darmo na VIXA."
                : "Nowości " + g[2] + " " + in + ": " + count + " " + tracksWord(count)
                + " do posłuchania i pobrania mp3, top 10 miesiąca i ranking. Muzyka na imprezę i do auta — za darmo na VIXA.";
    }

    // --- artists ---------------------------------------------------------------------------

    /** Every artist name (collaborations split) with its tracks; small catalogue, computed per call. */
    private Map<String, Artist> artistIndex() {
        Map<String, Artist> index = new HashMap<>();
        for (Object[] row : em.createQuery("select t.id, t.artist from Track t where " + PLAYABLE
                + " and t.artist is not null", Object[].class).getResultList()) {
            for (String name : splitArtists((String) row[1])) {
                String slug = slug(name);
                if (slug.length() < 2) {
                    continue;
                }
                index.computeIfAbsent(slug, s -> new Artist(s)).add(name, (Integer) row[0]);
            }
        }
        return index;
    }

    @Transactional(readOnly = true)
    public List<ArtistLink> artists() {
        return artistIndex().values().stream()
                .map(a -> new ArtistLink(a.slug, a.name(), a.trackIds.size()))
                .sorted(Comparator.comparing(ArtistLink::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtistPage artist(String slug) {
        Artist artist = artistIndex().get(slug);
        if (artist == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego wykonawcy");
        }
        List<Track> tracks = new ArrayList<>(em.createQuery("select t from Track t where t.id in :ids order by t.createdAt desc", Track.class)
                .setParameter("ids", artist.trackIds).getResultList());
        social.decorateAll(tracks);
        String name = artist.name();
        String title = name + " – utwory mp3, remiksy i nowości | VIXA";
        String description = name + " na VIXA: " + tracks.size() + " " + tracksWord(tracks.size())
                + " do posłuchania za darmo i pobrania mp3 — remiksy, extended mixy i nowości. Oceniaj i dodawaj do składanek.";
        return new ArtistPage(slug, name, tracks, new Seo(title, description, "/wykonawca/" + slug, true));
    }

    public static List<String> splitArtists(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ARTIST_SPLIT.split(raw.trim()))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    public static String slug(String name) {
        String plain = Normalizer.normalize(name.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    static String tracksWord(int n) {
        int tens = n % 100, ones = n % 10;
        if (n == 1) {
            return "utwór";
        }
        return ones >= 2 && ones <= 4 && (tens < 12 || tens > 14) ? "utwory" : "utworów";
    }

    private static final class Artist {
        final String slug;
        final Set<Integer> trackIds = new LinkedHashSet<>();
        final Map<String, Integer> spellings = new HashMap<>();

        Artist(String slug) {
            this.slug = slug;
        }

        void add(String name, Integer trackId) {
            trackIds.add(trackId);
            spellings.merge(name, 1, Integer::sum);
        }

        /** The spelling used most often. */
        String name() {
            return spellings.entrySet().stream().max(Map.Entry.<String, Integer>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey())).map(Map.Entry::getKey).orElse(slug);
        }
    }
}
