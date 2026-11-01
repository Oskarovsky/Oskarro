package com.oskarro.oskarito.seo;

import com.oskarro.oskarito.explore.ExploreService;
import com.oskarro.oskarito.explore.GenreContent;
import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.track.ChartService;
import com.oskarro.oskarito.track.CoverStorage;
import com.oskarro.oskarito.track.SetCategory;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.video.ChannelCatalog;
import com.oskarro.oskarito.video.ChannelVideo;
import com.oskarro.oskarito.video.Video;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-rendered entry HTML for every page (nginx sends page requests here via its @page location).
 * The Angular index.html gets the right title, description, canonical, Open Graph and JSON-LD,
 * plus the page's real content and links inside app-root, so search engines and link previews
 * see each page without running JavaScript. Angular replaces that content when it starts.
 */
@RestController
public class SeoPageController {

    /** Public origin. When the domain moves (vixa.com.pl), change it here, in SitemapController and in notify/NotificationService. */
    static final String SITE = "https://oskarro.com";
    private static final String LOGO = SITE + "/assets/logo.jpg";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Warsaw"));
    /** Year in titles: people search "vixa 2026", "muzyka klubowa 2026". */
    private static String year() {
        return String.valueOf(java.time.Year.now(ZoneId.of("Europe/Warsaw")).getValue());
    }

    private static String homeTitle() {
        return "Muzyka klubowa, vixa i disco polo " + year() + " – mp3 za darmo | VIXA";
    }
    private static final String HOME_DESCRIPTION = "VIXA — słuchaj z nami muzyki klubowej za darmo: club, vixa, trance, hands up i disco polo. "
            + "Oceniaj, komentuj i dodawaj swoje numery.";

    /** Public genres in site order: enum name, URL slug, label, one-line description. */
    public static final List<String[]> GENRES = List.of(
            new String[]{"CLUB", "club", "Club", "Club na imprezę z nami — sety i numery, które niosą parkiet od pierwszego bitu."},
            new String[]{"DANCE", "dance", "Dance", "Dance, przy którym śpiewasz z nami w aucie, w kuchni i na parkiecie."},
            new String[]{"RETRO", "retro", "Retro", "Retro, które wraca z nami na parkiet — hity, przy których bawili się wszyscy."},
            new String[]{"HOUSE", "house", "House", "House na długą noc z nami — groove, który trzyma parkiet do rana."},
            new String[]{"VIXA", "vixa", "Vixa", "Vixa, z którą rozkręcasz z nami każdą imprezę — szybkie bity i pompowane wokale."},
            new String[]{"BASS_HOUSE", "bass-house", "Bass House", "Bass house na siłownię i parkiet — mocny bas, który pcha Cię z nami do przodu."},
            new String[]{"TECHNO", "techno", "Techno", "Techno na nocną jazdę z nami — powtarzalny bit, w którym łatwo się zatracić."},
            new String[]{"BIG_ROOM", "big-room", "Big Room", "Big room, przy którym skaczesz z nami jak pod festiwalową sceną."},
            new String[]{"TRANCE", "trance", "Trance", "Trance na trening, drogę i noc z nami — długie budowanie i melodia, która niesie."},
            new String[]{"ELECTRO", "electro", "Electro", "Electro z klubową energią — włącz je z nami i podkręć tempo."},
            new String[]{"HANDS_UP", "hands-up", "Hands Up", "Hands up — refreny, przy których cała sala podnosi z nami ręce."},
            new String[]{"POP", "pop", "Pop", "Pop, który śpiewasz z nami — hity z radia i list przebojów, które zna każdy."},
            new String[]{"DISCO_POLO", "disco-polo", "Disco Polo", "Disco polo na wesele, poprawiny i domówkę — bawisz się z nami do białego rana."});

    private static final Map<SetCategory, String[]> SET_LABELS = Map.of(
            SetCategory.USERS, new String[]{"Sety użytkowników", "Miksy nagrane przez didżejów z naszej społeczności."},
            SetCategory.CLUBS, new String[]{"Sety z klubów", "Sety nagrane na żywo w klubach."},
            SetCategory.FESTIVALS, new String[]{"Sety z festiwali", "Występy z dużych scen i festiwali."},
            SetCategory.RADIO, new String[]{"Audycje radiowe", "Nagrania audycji i radiowych mixów."});

    /** Pages users reach only signed in, or that make no sense in search results. */
    private static final Pattern PRIVATE = Pattern.compile(
            "^/(login|register|przypomnij|haslo|aktywuj|logowanie/.*|profile(/.*)?|konto(/.*)?|moja-aktywnosc|admin(/.*)?|mod(/.*)?|user(/.*)?|toplist(/.*)?|playlist/all/[^/]+|szukaj|zglos-naruszenie)$|.*/(add|edit)$");
    private static final Pattern TRACK = Pattern.compile("^/track/(\\d{1,9})$");
    private static final Pattern GENRE = Pattern.compile("^/tracklist/([a-z0-9-]{1,30})$");
    private static final Pattern SETS = Pattern.compile("^/sety(?:/([a-z0-9-]{1,30}))?$");
    private static final Pattern PLAYLIST = Pattern.compile("^/playlist/(\\d{1,9})/details$");
    private static final Pattern VIDEO_SET = Pattern.compile("^/video/set/([A-Za-z0-9_-]{11})$");
    private static final Pattern MONTH_PAGE = Pattern.compile("^/nowosci/([a-z0-9-]{1,30})/(\\d{4}-\\d{2})$");
    private static final Pattern MONTH_HUB = Pattern.compile("^/nowosci/([a-z0-9-]{1,30})$");
    private static final Pattern ARTIST = Pattern.compile("^/wykonawca/([a-z0-9-]{1,80})$");
    private static final Pattern RANKING = Pattern.compile("^/ranking/([a-z0-9-]{1,30})(?:/(miesiac|ogolny))?$");
    private static final Pattern VIDEO = Pattern.compile("^/video/(\\d{1,9})/details$");
    private static final Pattern ANY_GENRE = Pattern.compile("^/tracklist/([A-Za-z0-9_-]{1,30})$");
    /** Old genre slugs Google still knows, mapped to today's ones. */
    private static final Map<String, String> GENRE_ALIASES = Map.of(
            "disco", "disco-polo", "discopolo", "disco-polo", "bigroom", "big-room", "handsup", "hands-up",
            "electro-house", "electro", "set", "/sety", "sets", "/sety");

    private static final Map<String, String[]> STATIC_PAGES = Map.of(
            "/ranking", new String[]{"Ranking użytkowników i najlepsze numery | VIXA", "Kto dodaje najwięcej muzyki klubowej na VIXA i które numery biorą najwięcej głosów.", "Ranking"},
            "/radio", new String[]{"Radio z muzyką klubową, vixą i disco polo | VIXA", "Włącz radio VIXA i słuchaj z nami muzyki klubowej na imprezę — wybierz gatunki albo swoją składankę. Kolejny numer startuje sam.", "Radio VIXA"},
            "/video", new String[]{"Sety wideo na imprezę | VIXA", "Sety VIXA na imprezę, do auta i na parkiet. Mixy klubowe z kanału.", "Sety wideo"},
            "/playlist/all", new String[]{"Playlisty i składanki na imprezę | VIXA", "Składanki na imprezę, do auta i na siłownię, ułożone przez ludzi z VIXA.", "Playlisty"},
            "/tracklist", new String[]{"Wszystkie utwory — club, vixa, trance i disco polo za darmo | VIXA", "Wszystkie numery dodane przez społeczność: club, dance, vixa, trance, hands up, disco polo i więcej. Słuchaj z nami za darmo.", "Wszystkie utwory"},
            "/regulamin", new String[]{"Regulamin | VIXA", "Regulamin serwisu VIXA.", "Regulamin"},
            "/polityka-prywatnosci", new String[]{"Polityka prywatności | VIXA", "Jak VIXA przetwarza dane osobowe.", "Polityka prywatności"},
            "/cookies", new String[]{"Pliki cookies | VIXA", "Jakich plików cookies używa VIXA.", "Pliki cookies"});

    @PersistenceContext
    private EntityManager em;

    private final CoverStorage covers;
    private final ChannelCatalog channel;
    private final ExploreService explore;
    private final ChartService charts;
    private final ObjectMapper json;
    private final Path indexFile;
    private volatile String cachedIndex;
    private volatile FileTime cachedAt;

    public SeoPageController(CoverStorage covers, ChannelCatalog channel, ExploreService explore, ChartService charts, ObjectMapper json,
                             @Value("${oskarro.web.index:/var/www/oskarro/index.html}") String indexFile) {
        this.covers = covers;
        this.channel = channel;
        this.explore = explore;
        this.charts = charts;
        this.json = json;
        this.indexFile = Path.of(indexFile);
    }

    record Page(int status, String title, String description, String path, boolean index,
                String image, String type, Object jsonLd, String body) {
    }

    @GetMapping(value = {"/api/seo/page", "/api/seo/page/**"}, produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> page(HttpServletRequest request) {
        String path = request.getRequestURI().substring("/api/seo/page".length());
        if (path.isEmpty() || path.length() > 200) {
            path = "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String moved = movedTo(path);
        if (moved != null) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, SITE + moved)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                    .build();
        }
        Page page = build(path);
        return ResponseEntity.status(page.status())
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(render(page));
    }

    /** Old addresses that still get visits from search engines: 301 to where the page lives now. */
    static String movedTo(String path) {
        Matcher m;
        if ((m = Pattern.compile("^/tracks/(\\d{1,9})$").matcher(path)).matches()) {
            return "/track/" + m.group(1);
        }
        if (path.startsWith("/video/category/")) {
            return "/video";
        }
        if ((m = ANY_GENRE.matcher(path)).matches()) {
            String raw = m.group(1);
            String slug = raw.toLowerCase(Locale.ROOT).replace('_', '-');
            slug = GENRE_ALIASES.getOrDefault(slug, slug);
            if (slug.startsWith("/")) {
                return slug;
            }
            String known = slug;
            if (!slug.equals(raw) && GENRES.stream().anyMatch(g -> g[1].equals(known))) {
                return "/tracklist/" + slug;
            }
        }
        return null;
    }

    Page build(String path) {
        if (path.equals("/")) {
            return home();
        }
        Matcher m;
        if ((m = TRACK.matcher(path)).matches()) {
            return track(Integer.parseInt(m.group(1)));
        }
        if ((m = GENRE.matcher(path)).matches()) {
            return genre(m.group(1));
        }
        if ((m = SETS.matcher(path)).matches()) {
            return sets(m.group(1));
        }
        if ((m = PLAYLIST.matcher(path)).matches()) {
            return playlist(Integer.parseInt(m.group(1)));
        }
        if ((m = VIDEO_SET.matcher(path)).matches()) {
            return videoSet(m.group(1));
        }
        if ((m = MONTH_PAGE.matcher(path)).matches()) {
            return monthPage(m.group(1), m.group(2));
        }
        if ((m = MONTH_HUB.matcher(path)).matches()) {
            return monthHub(m.group(1));
        }
        if ((m = ARTIST.matcher(path)).matches()) {
            return artistPage(m.group(1));
        }
        if ((m = RANKING.matcher(path)).matches()) {
            return rankingPage(m.group(1), m.group(2));
        }
        if ((m = VIDEO.matcher(path)).matches()) {
            return video(Integer.parseInt(m.group(1)));
        }
        String[] known = STATIC_PAGES.get(path);
        if (known != null) {
            String extra = switch (path) {
                case "/video" -> videoLinks(null, 60);
                case "/tracklist" -> trackList(latest(null, false, 60));
                case "/ranking" -> "<h2>Top 10 tygodnia w każdym gatunku</h2>" + rankingLinks();
                default -> "";
            };
            return new Page(200, known[0], known[1], path, true, LOGO, "website", null,
                    "<h1>" + esc(known[2]) + "</h1><p>" + esc(known[1]) + "</p>" + extra + genreLinks());
        }
        if (PRIVATE.matcher(path).matches()) {
            return new Page(200, "VIXA", HOME_DESCRIPTION, path, false, LOGO, "website", null, "");
        }
        return notFound(path, "Nie ma takiej strony");
    }

    private Page home() {
        StringBuilder body = new StringBuilder("<h1>Muzyka klubowa, vixa i disco polo — słuchaj z nami</h1><p>")
                .append(esc(HOME_DESCRIPTION)).append("</p><h2>Gatunki</h2>").append(genreLinks())
                .append("<h2>Ostatnio dodane</h2>").append(trackList(latest(null, false, 30)))
                .append("<h2>Sety i mixy</h2>").append(trackList(latest(null, true, 10)))
                .append("<h2>Nowości miesiąca</h2>").append(newsLinks())
                .append("<h2>Top 10 tygodnia</h2>").append(rankingLinks())
                .append("<p><a href=\"/sety\">Wszystkie sety</a> · <a href=\"/radio\">Radio</a> · <a href=\"/ranking\">Ranking</a> · <a href=\"/playlist/all\">Playlisty</a></p>");
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "WebSite");
        ld.put("name", "VIXA");
        ld.put("url", SITE + "/");
        ld.put("description", HOME_DESCRIPTION);
        ld.put("inLanguage", "pl");
        ld.put("potentialAction", Map.of("@type", "SearchAction",
                "target", SITE + "/szukaj?q={search_term_string}", "query-input", "required name=search_term_string"));
        return new Page(200, homeTitle(), HOME_DESCRIPTION, "/", true, LOGO, "website", ld, body.toString());
    }

    private Page track(Integer id) {
        Track track = em.find(Track.class, id);
        if (track == null || blank(track.getUrl()) || blank(track.getGenre())) {
            return notFound("/track/" + id, "Nie ma takiego utworu");
        }
        boolean set = "set".equalsIgnoreCase(track.getKind());
        String artist = blank(track.getArtist()) ? "Nieznany wykonawca" : track.getArtist().trim();
        String title = blank(track.getTitle()) ? "Bez tytułu" : track.getTitle().trim();
        String version = blank(track.getVersion()) ? "" : " (" + track.getVersion().trim() + ")";
        String[] genre = genreOf(track.getGenre());
        String full = artist + " – " + title + version;
        String path = "/track/" + id;
        String added = track.getCreatedAt() == null ? "" : DAY.format(track.getCreatedAt());
        String section = set ? setLabel(track.getSetCategory()) : genre[2];
        String description = cut((set ? "Posłuchaj seta za darmo: " : "Posłuchaj i pobierz mp3: ") + full + ". "
                + section + " na VIXA" + (added.isEmpty() ? "" : ", dodane " + added)
                + ". Oceniaj, komentuj i zapisz do swojej składanki.", 158);
        String image = covers.exists(id) ? SITE + "/api/tracks/" + id + "/cover" : LOGO;
        String author = track.getAuthor() == null ? null : track.getAuthor().getUsername();

        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › ");
        if (set) {
            body.append("<a href=\"/sety\">Sety</a>");
        } else {
            body.append("<a href=\"/tracklist/").append(genre[1]).append("\">").append(esc(genre[2])).append("</a>");
        }
        body.append("</nav><h1>").append(esc(title)).append(esc(version)).append("</h1><p><strong>").append(artistLinks(artist)).append("</strong></p><p>")
                .append(esc(section));
        if (!added.isEmpty()) {
            body.append(" · dodano ").append(added);
        }
        if (author != null) {
            body.append(" · dodał ").append(esc(author));
        }
        body.append("</p>");
        if (!blank(track.getEventName())) {
            body.append("<p>").append(esc(track.getEventName())).append("</p>");
        }
        if (!blank(track.getTracklist())) {
            body.append("<h2>Tracklista</h2><pre>").append(esc(track.getTracklist())).append("</pre>");
        }
        List<Object[]> byArtist = em.createQuery("select t.id, t.artist, t.title, t.version from Track t where t.id <> :id"
                        + " and lower(t.artist) = lower(:artist) and t.url is not null and t.url <> '' order by t.id desc", Object[].class)
                .setParameter("id", id).setParameter("artist", artist).setMaxResults(10).getResultList();
        if (!byArtist.isEmpty()) {
            body.append("<h2>Więcej: ").append(esc(artist)).append("</h2>").append(trackList(byArtist));
        }
        List<Object[]> similar = em.createQuery("select t.id, t.artist, t.title, t.version from Track t where t.id <> :id"
                        + " and upper(t.genre) = :genre and t.url is not null and t.url <> ''"
                        + " and (t.kind is null or t.kind <> 'set') order by t.id desc", Object[].class)
                .setParameter("id", id).setParameter("genre", genre[0]).setMaxResults(12).getResultList();
        if (!similar.isEmpty()) {
            body.append("<h2>Więcej ").append(esc(genre[2])).append("</h2>").append(trackList(similar));
        }

        Map<String, Object> recording = new LinkedHashMap<>();
        recording.put("@context", "https://schema.org");
        recording.put("@type", "MusicRecording");
        recording.put("name", title + version);
        recording.put("byArtist", Map.of("@type", "MusicGroup", "name", artist));
        recording.put("genre", genre[2]);
        recording.put("url", SITE + path);
        recording.put("image", image);
        recording.put("inLanguage", "pl");
        if (track.getCreatedAt() != null) {
            recording.put("datePublished", track.getCreatedAt().toString().substring(0, 10));
        }
        Map<String, Object> crumbs = breadcrumbs(set ? new String[][]{{"Sety", "/sety"}, {full, path}}
                : new String[][]{{genre[2], "/tracklist/" + genre[1]}, {full, path}});
        return new Page(200, full + " | VIXA", description, path, true, image, "music.song",
                List.of(recording, crumbs), body.toString());
    }

    private Page genre(String slug) {
        String[] genre = GENRES.stream().filter(g -> g[1].equals(slug)).findFirst().orElse(null);
        if (genre == null) {
            return notFound("/tracklist/" + slug, "Nie ma takiego gatunku");
        }
        String path = "/tracklist/" + slug;
        String heading = genre[0].equals("CLUB") ? "Muzyka klubowa — Club" : genre[2] + " na imprezę";
        String title = genreTitle(genre[0], genre[2]);
        GenreContent.Content content = GenreContent.of(slug);
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › ").append(esc(genre[2]))
                .append("</nav><h1>").append(esc(heading)).append("</h1><p>").append(esc(genre[3])).append("</p>")
                .append("<p><a href=\"/nowosci/").append(slug).append("\">Nowości ").append(esc(genre[2])).append(" miesiąc po miesiącu</a> · ")
                .append("<a href=\"/ranking/").append(slug).append("\">Top 10 ").append(esc(genre[2])).append(" tygodnia</a></p>")
                .append("<h2>Najnowsze: ").append(esc(genre[2])).append("</h2>").append(trackList(latest(genre[0], false, 40)));
        List<Object> ld = new ArrayList<>();
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("@context", "https://schema.org");
        page.put("@type", "CollectionPage");
        page.put("name", heading);
        page.put("description", genre[3]);
        page.put("url", SITE + path);
        page.put("inLanguage", "pl");
        page.put("isPartOf", Map.of("@type", "WebSite", "name", "VIXA", "url", SITE + "/"));
        ld.add(page);
        ld.add(breadcrumbs(new String[][]{{genre[2], path}}));
        if (content != null) {
            body.append("<h2>").append(esc(genre[2])).append(" — o gatunku</h2>");
            content.intro().forEach(p -> body.append("<p>").append(esc(p)).append("</p>"));
            body.append("<h2>Najczęstsze pytania</h2>");
            content.faq().forEach(f -> body.append("<h3>").append(esc(f.q())).append("</h3><p>").append(esc(f.a())).append("</p>"));
            ld.add(faq(content.faq()));
        }
        body.append("<h2>Inne gatunki</h2>").append(genreLinks());
        return new Page(200, title, genre[3], path, true, LOGO, "website", ld, body.toString());
    }


    /** Genre titles carry the year and the words people type: mp3, na imprezę, do auta. */
    static String genreTitle(String id, String label) {
        return switch (id) {
            case "CLUB" -> "Muzyka klubowa " + year() + " – nowości club mp3 na imprezę | VIXA";
            case "VIXA" -> "Vixa " + year() + " – pompa do auta i na imprezę, mp3 | VIXA";
            case "DISCO_POLO" -> "Disco polo " + year() + " – nowości mp3 na wesele i imprezę | VIXA";
            default -> label + " " + year() + " – nowości mp3 na imprezę i do auta | VIXA";
        };
    }

    private Page sets(String slug) {
        SetCategory category = null;
        if (slug != null) {
            category = SetCategory.parse(slug).filter(c -> c.slug().equals(slug)).orElse(null);
            if (category == null) {
                return notFound("/sety/" + slug, "Nie ma takiej kategorii setów");
            }
        }
        String path = category == null ? "/sety" : "/sety/" + category.slug();
        String heading = category == null ? "Sety i mixy" : SET_LABELS.get(category)[0];
        String description = category == null
                ? "Sety DJ-skie i mixy z klubową muzyką, vixą i disco polo: sety użytkowników, z klubów, z festiwali i audycje radiowe. Słuchaj z nami za darmo."
                : SET_LABELS.get(category)[1] + " Słuchaj z nami za darmo na VIXA.";
        String query = "select t.id, t.artist, t.title, t.version from Track t where t.kind = 'set' and t.url is not null and t.url <> ''"
                + (category == null ? "" : " and t.setCategory = :category") + " order by t.id desc";
        var q = em.createQuery(query, Object[].class).setMaxResults(40);
        if (category != null) {
            q.setParameter("category", category.name());
        }
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › ")
                .append(category == null ? "Sety" : "<a href=\"/sety\">Sety</a> › " + esc(heading))
                .append("</nav><h1>").append(esc(heading)).append("</h1><p>").append(esc(description)).append("</p><ul>");
        for (SetCategory c : SetCategory.values()) {
            body.append("<li><a href=\"/sety/").append(c.slug()).append("\">").append(esc(SET_LABELS.get(c)[0])).append("</a></li>");
        }
        body.append("</ul>").append(trackList(q.getResultList()));
        return new Page(200, heading + " — słuchaj za darmo | VIXA", description, path, true, LOGO, "website", null, body.toString());
    }

    private Page playlist(Integer id) {
        Playlist playlist = em.find(Playlist.class, id);
        if (playlist == null || Boolean.TRUE.equals(playlist.getPrivatePlaylist())) {
            return notFound("/playlist/" + id + "/details", "Nie ma takiej składanki");
        }
        String path = "/playlist/" + id + "/details";
        String name = blank(playlist.getName()) ? "Składanka" : playlist.getName().trim();
        String owner = playlist.getUser() == null ? "VIXA" : playlist.getUser().getUsername();
        List<Object[]> tracks = em.createQuery("select t.id, t.artist, t.title, t.version from PlaylistEntry e join e.track t"
                + " where e.playlist.id = :id order by e.id", Object[].class).setParameter("id", id).setMaxResults(100).getResultList();
        String description = cut(blank(playlist.getDescription())
                ? "Składanka „" + name + "” od " + owner + " na VIXA — " + tracks.size() + " numerów do posłuchania za darmo."
                : playlist.getDescription().trim() + " — składanka od " + owner + " na VIXA.", 158);
        String body = "<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/playlist/all\">Playlisty</a></nav><h1>" + esc(name)
                + "</h1><p>" + esc(description) + "</p>" + trackList(tracks);
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "MusicPlaylist");
        ld.put("name", name);
        ld.put("url", SITE + path);
        ld.put("numTracks", tracks.size());
        ld.put("track", tracks.stream().limit(30).map(row -> Map.of("@type", "MusicRecording",
                "name", str(row[2]) + (blank(str(row[3])) ? "" : " (" + str(row[3]) + ")"),
                "byArtist", Map.of("@type", "MusicGroup", "name", str(row[1])),
                "url", SITE + "/track/" + row[0])).toList());
        return new Page(200, name + " — składanka od " + owner + " | VIXA", description, path, true, LOGO, "music.playlist", ld, body);
    }

    private Page videoSet(String id) {
        String path = "/video/set/" + id;
        List<ChannelVideo> all = channelVideos();
        if (all == null) {
            // YouTube is unreachable for the moment: still a normal page, just without details.
            return new Page(200, "Set wideo | VIXA", "Set z kanału VIXA na YouTube — muzyka klubowa i vixa na imprezę.",
                    path, true, "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg", "video.other", null, "<h1>Set wideo</h1>");
        }
        ChannelVideo video = all.stream().filter(v -> id.equals(v.id())).findFirst().orElse(null);
        if (video == null) {
            return notFound(path, "Nie ma takiego seta");
        }
        String title = blank(video.title()) ? "Set wideo" : video.title().trim();
        String description = cut("Obejrzyj i posłuchaj za darmo: " + title
                + (blank(video.duration()) ? "" : " (" + video.duration() + ")") + ". Set z kanału na YouTube — muzyka na imprezę z VIXA.", 158);
        String image = "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/video\">Sety wideo</a></nav><h1>")
                .append(esc(title)).append("</h1><p>");
        List<String> facts = new ArrayList<>();
        for (String fact : new String[]{video.duration(), video.published(), video.views()}) {
            if (!blank(fact)) {
                facts.add(esc(fact));
            }
        }
        body.append(String.join(" · ", facts)).append("</p><p><a href=\"https://www.youtube.com/watch?v=").append(id)
                .append("\" rel=\"noopener\">Zobacz na YouTube</a></p><h2>Więcej setów</h2>").append(videoLinks(id, 20));
        return new Page(200, cut(title, 70) + " | VIXA", description, path, true, image, "video.other",
                breadcrumbs("VIXA", new String[][]{{"Sety wideo", "/video"}, {title, path}}), body.toString());
    }

    private Page video(Integer id) {
        Video video = em.find(Video.class, id);
        if (video == null) {
            return notFound("/video/" + id + "/details", "Nie ma takiego wideo");
        }
        String path = "/video/" + id + "/details";
        String name = blank(video.getName()) ? "Wideo" : video.getName().trim();
        String description = cut(name + " — wideo z muzyką na imprezę na VIXA.", 158);
        return new Page(200, cut(name, 70) + " | VIXA", description, path, true, LOGO, "video.other", null,
                "<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/video\">Wideo</a></nav><h1>" + esc(name) + "</h1>"
                        + videoLinks(null, 20));
    }

    private List<ChannelVideo> channelVideos() {
        try {
            return channel.videos();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private String videoLinks(String skip, int limit) {
        List<ChannelVideo> all = channelVideos();
        if (all == null || all.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("<ul>");
        all.stream().filter(v -> !v.id().equals(skip)).limit(limit).forEach(v -> out.append("<li><a href=\"/video/set/")
                .append(v.id()).append("\">").append(esc(blank(v.title()) ? "Set wideo" : v.title().trim())).append("</a></li>"));
        return out.append("</ul>").toString();
    }

    private Page monthPage(String slug, String month) {
        ExploreService.MonthPage page;
        try {
            page = explore.month(slug, month);
        } catch (ResponseStatusException missing) {
            return notFound("/nowosci/" + slug + "/" + month, "Nie ma takiej strony z nowościami");
        }
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/tracklist/")
                .append(slug).append("\">").append(esc(page.genreLabel())).append("</a> › <a href=\"/nowosci/").append(slug)
                .append("\">Nowości</a></nav><h1>").append(esc(page.genreLabel())).append(" — nowości ").append(esc(page.label()))
                .append("</h1><p>").append(esc(page.seo().description())).append("</p>");
        if (!page.top().isEmpty()) {
            body.append("<h2>Top 10 — ").append(esc(page.label())).append("</h2><ol>");
            page.top().forEach(row -> body.append("<li><a href=\"/track/").append(row.track().getId()).append("\">")
                    .append(esc(trackName(row.track()))).append("</a> — ").append(row.points()).append(" pkt</li>"));
            body.append("</ol>");
        }
        body.append("<h2>Wszystkie nowości</h2>").append(trackList(page.tracks().stream()
                .map(t -> new Object[]{t.getId(), t.getArtist(), t.getTitle(), t.getVersion()}).toList()));
        body.append("<p>");
        if (page.previous() != null) {
            body.append("<a href=\"/nowosci/").append(slug).append("/").append(page.previous().month()).append("\">← ")
                    .append(esc(page.previous().label())).append("</a> ");
        }
        if (page.next() != null) {
            body.append("<a href=\"/nowosci/").append(slug).append("/").append(page.next().month()).append("\">")
                    .append(esc(page.next().label())).append(" →</a>");
        }
        body.append("</p>");
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("@context", "https://schema.org");
        list.put("@type", "ItemList");
        list.put("name", page.seo().title());
        list.put("itemListElement", page.tracks().stream().limit(50).map(t -> Map.of("@type", "ListItem",
                "position", page.tracks().indexOf(t) + 1, "url", SITE + "/track/" + t.getId(), "name", trackName(t))).toList());
        return new Page(200, page.seo().title(), page.seo().description(), page.seo().path(), page.seo().index(), LOGO, "website",
                List.of(list, breadcrumbs(new String[][]{{page.genreLabel(), "/tracklist/" + slug},
                        {"Nowości " + page.label(), page.seo().path()}})), body.toString());
    }

    private Page monthHub(String slug) {
        ExploreService.GenreInfo info;
        try {
            info = explore.genreInfo(slug);
        } catch (ResponseStatusException missing) {
            return notFound("/nowosci/" + slug, "Nie ma takiego gatunku");
        }
        String path = "/nowosci/" + slug;
        String title = "Nowości " + info.label() + " " + year() + " – miesiąc po miesiącu, mp3 | VIXA";
        String description = "Nowe numery " + info.label() + " z każdego miesiąca: listy nowości, top 10 miesiąca i ranking. Słuchaj za darmo i pobieraj mp3 na VIXA.";
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/tracklist/")
                .append(slug).append("\">").append(esc(info.label())).append("</a></nav><h1>Nowości ").append(esc(info.label()))
                .append("</h1><p>").append(esc(description)).append("</p><ul>");
        info.months().forEach(mo -> body.append("<li><a href=\"/nowosci/").append(slug).append("/").append(mo.month()).append("\">")
                .append(esc(info.label())).append(" ").append(esc(mo.label())).append("</a> (").append(mo.tracks()).append(")</li>"));
        body.append("</ul>");
        return new Page(200, title, description, path, !info.months().isEmpty(), LOGO, "website",
                breadcrumbs(new String[][]{{info.label(), "/tracklist/" + slug}, {"Nowości", path}}), body.toString());
    }

    private Page artistPage(String slug) {
        ExploreService.ArtistPage page;
        try {
            page = explore.artist(slug);
        } catch (ResponseStatusException missing) {
            return notFound("/wykonawca/" + slug, "Nie ma takiego wykonawcy");
        }
        String body = "<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › Wykonawcy</nav><h1>" + esc(page.name())
                + "</h1><p>" + esc(page.seo().description()) + "</p>" + trackList(page.tracks().stream()
                .map(t -> new Object[]{t.getId(), t.getArtist(), t.getTitle(), t.getVersion()}).toList()) + genreLinks();
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "MusicGroup");
        ld.put("name", page.name());
        ld.put("url", SITE + page.seo().path());
        ld.put("track", page.tracks().stream().limit(30).map(t -> Map.of("@type", "MusicRecording",
                "name", trackName(t), "url", SITE + "/track/" + t.getId())).toList());
        return new Page(200, page.seo().title(), page.seo().description(), page.seo().path(), true, LOGO, "profile",
                List.of(ld, breadcrumbs(new String[][]{{page.name(), page.seo().path()}})), body);
    }

    private Page rankingPage(String category, String okres) {
        boolean sets = "sety".equals(category);
        String[] genre = sets ? null : GENRES.stream().filter(g -> g[1].equals(category)).findFirst().orElse(null);
        String path = "/ranking/" + category + (okres == null ? "" : "/" + okres);
        if (!sets && genre == null) {
            return notFound(path, "Nie ma takiego rankingu");
        }
        String label = sets ? "setów" : genre[2];
        String when = okres == null ? "tygodnia" : okres.equals("miesiac") ? "miesiąca" : "wszech czasów";
        String period = okres == null ? "week" : okres.equals("miesiac") ? "month" : "all";
        ChartService.Chart chart = charts.chart(period, category);
        String title = "Top 10 " + label + " " + when + " – ranking hitów " + year() + " | VIXA";
        String description = "Ranking " + label + " " + when + " na VIXA: 10 numerów z największą liczbą głosów i odsłuchów. Słuchaj za darmo i pobieraj mp3.";
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">VIXA</a> › <a href=\"/ranking\">Ranking</a></nav><h1>Top 10 ")
                .append(esc(label)).append(" ").append(when).append("</h1><p>").append(esc(description)).append("</p><ol>");
        chart.rows().forEach(row -> body.append("<li><a href=\"/track/").append(row.track().getId()).append("\">")
                .append(esc(trackName(row.track()))).append("</a> — ").append(row.points()).append(" pkt</li>"));
        body.append("</ol><p><a href=\"/ranking/").append(category).append("\">Tydzień</a> · <a href=\"/ranking/").append(category)
                .append("/miesiac\">Miesiąc</a> · <a href=\"/ranking/").append(category).append("/ogolny\">Ogólny</a></p>")
                .append("<h2>Rankingi innych gatunków</h2>").append(rankingLinks());
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("@context", "https://schema.org");
        list.put("@type", "ItemList");
        list.put("name", title);
        list.put("itemListOrder", "https://schema.org/ItemListOrderDescending");
        list.put("itemListElement", chart.rows().stream().map(row -> Map.of("@type", "ListItem", "position", row.place(),
                "url", SITE + "/track/" + row.track().getId(), "name", trackName(row.track()))).toList());
        return new Page(200, title, description, path, true, LOGO, "website",
                List.of(list, breadcrumbs(new String[][]{{"Ranking", "/ranking"}, {"Top 10 " + label + " " + when, path}})), body.toString());
    }

    private static String trackName(Track t) {
        String artist = blank(t.getArtist()) ? "Nieznany wykonawca" : t.getArtist().trim();
        String title = blank(t.getTitle()) ? "Bez tytułu" : t.getTitle().trim();
        return artist + " – " + title + (blank(t.getVersion()) ? "" : " (" + t.getVersion().trim() + ")");
    }

    private static String artistLinks(String artist) {
        List<String> names = ExploreService.splitArtists(artist);
        if (names.size() < 1) {
            return esc(artist);
        }
        // Link each artist, keep the original separators readable.
        String out = esc(artist);
        for (String name : names) {
            String slug = ExploreService.slug(name);
            if (slug.length() >= 2) {
                out = out.replaceFirst(Pattern.quote(esc(name)), Matcher.quoteReplacement(
                        "<a href=\"/wykonawca/" + slug + "\">" + esc(name) + "</a>"));
            }
        }
        return out;
    }

    private static String newsLinks() {
        String month = java.time.YearMonth.now(ExploreService.WARSAW).toString();
        StringBuilder out = new StringBuilder("<ul>");
        for (String[] g : GENRES) {
            out.append("<li><a href=\"/nowosci/").append(g[1]).append("\">Nowości ").append(esc(g[2])).append(" ")
                    .append(esc(ExploreService.monthLabel(java.time.YearMonth.parse(month)))).append("</a></li>");
        }
        return out.append("</ul>").toString();
    }

    private static String rankingLinks() {
        StringBuilder out = new StringBuilder("<ul>");
        for (String[] g : GENRES) {
            out.append("<li><a href=\"/ranking/").append(g[1]).append("\">Top 10 ").append(esc(g[2])).append(" tygodnia</a></li>");
        }
        out.append("<li><a href=\"/ranking/sety\">Top 10 setów tygodnia</a></li>");
        return out.append("</ul>").toString();
    }

    private static Map<String, Object> faq(List<GenreContent.Faq> items) {
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "FAQPage");
        ld.put("mainEntity", items.stream().map(f -> Map.of("@type", "Question", "name", f.q(),
                "acceptedAnswer", Map.of("@type", "Answer", "text", f.a()))).toList());
        return ld;
    }

    private Page notFound(String path, String message) {
        return new Page(404, message + " | VIXA", message + ".", path, false, LOGO, "website", null,
                "<h1>" + esc(message) + "</h1><p><a href=\"/\">Wróć na stronę główną VIXA</a></p>" + genreLinks());
    }

    // --- rendering -------------------------------------------------------------------------

    String render(Page page) {
        String canonical = SITE + page.path();
        StringBuilder head = new StringBuilder()
                .append("<title>").append(esc(page.title())).append("</title>\n")
                .append("  <meta name=\"description\" content=\"").append(esc(page.description())).append("\">\n")
                .append("  <meta name=\"robots\" content=\"").append(page.index() ? "index, follow, max-image-preview:large" : "noindex, follow").append("\">\n");
        if (page.index()) {
            head.append("  <link rel=\"canonical\" href=\"").append(esc(canonical)).append("\">\n");
        }
        head.append("  <meta property=\"og:type\" content=\"").append(page.type()).append("\">\n")
                .append("  <meta property=\"og:locale\" content=\"pl_PL\">\n")
                .append("  <meta property=\"og:site_name\" content=\"VIXA\">\n")
                .append("  <meta property=\"og:title\" content=\"").append(esc(page.title())).append("\">\n")
                .append("  <meta property=\"og:description\" content=\"").append(esc(page.description())).append("\">\n")
                .append("  <meta property=\"og:url\" content=\"").append(esc(canonical)).append("\">\n")
                .append("  <meta property=\"og:image\" content=\"").append(esc(page.image())).append("\">\n")
                .append("  <meta name=\"twitter:card\" content=\"").append(LOGO.equals(page.image()) ? "summary" : "summary_large_image").append("\">\n")
                .append("  <meta name=\"twitter:title\" content=\"").append(esc(page.title())).append("\">\n")
                .append("  <meta name=\"twitter:description\" content=\"").append(esc(page.description())).append("\">\n")
                .append("  <meta name=\"twitter:image\" content=\"").append(esc(page.image())).append("\">\n");
        if (page.jsonLd() != null) {
            head.append("  <script type=\"application/ld+json\" id=\"oskarro-ld\">")
                    .append(json.writeValueAsString(page.jsonLd()).replace("<", "\\u003c")).append("</script>\n");
        }
        // The content below is for crawlers and for browsers without JavaScript; with JavaScript
        // the app takes over at once, so it is hidden to avoid a flash before Angular starts.
        head.append("  <script>document.documentElement.classList.add('js')</script>\n")
                .append("  <style>.js .seo-pre{display:none}.seo-pre{max-width:1180px;margin:28px auto;padding:0 20px;font-family:system-ui,sans-serif;color:#efe9dc;background:#12110e}.seo-pre a{color:#e4ff4a}</style>\n");

        String html = index();
        html = html.replaceAll("(?s)<title>.*?</title>\\s*", "")
                .replaceAll("<meta name=\"(description|robots|twitter:[a-z]+)\"[^>]*>\\s*", "")
                .replaceAll("<meta property=\"og:[a-z_]+\"[^>]*>\\s*", "")
                .replaceAll("<link rel=\"canonical\"[^>]*>\\s*", "")
                .replaceAll("(?s)<script type=\"application/ld\\+json\"[^>]*>.*?</script>\\s*", "");
        html = html.replaceFirst("(<meta charset=\"utf-8\">)", "$1\n  " + Matcher.quoteReplacement(head.toString()));
        return html.replaceFirst("(<app-root[^>]*>)\\s*(</app-root>)",
                "$1" + Matcher.quoteReplacement("<div class=\"seo-pre\">" + page.body() + "</div>") + "$2");
    }

    private String index() {
        try {
            FileTime modified = Files.getLastModifiedTime(indexFile);
            if (cachedIndex == null || !modified.equals(cachedAt)) {
                cachedIndex = Files.readString(indexFile, StandardCharsets.UTF_8);
                cachedAt = modified;
            }
            return cachedIndex;
        } catch (IOException missing) {
            // Tests and local runs have no built frontend: a bare shell keeps the output well-formed.
            return "<!doctype html>\n<html lang=\"pl\">\n<head>\n  <meta charset=\"utf-8\">\n  <base href=\"/\">\n</head>\n"
                    + "<body>\n  <app-root></app-root>\n</body>\n</html>\n";
        }
    }

    // --- helpers -----------------------------------------------------------------------------

    private List<Object[]> latest(String genre, boolean sets, int limit) {
        String query = "select t.id, t.artist, t.title, t.version from Track t where t.url is not null and t.url <> ''"
                + " and t.genre is not null and t.genre <> ''"
                + (sets ? " and t.kind = 'set'" : " and (t.kind is null or t.kind <> 'set')")
                + (genre == null ? "" : " and upper(t.genre) = :genre") + " order by t.id desc";
        var q = em.createQuery(query, Object[].class).setMaxResults(limit);
        if (genre != null) {
            q.setParameter("genre", genre);
        }
        return q.getResultList();
    }

    private static String trackList(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("<ul>");
        for (Object[] row : rows) {
            String artist = blank(str(row[1])) ? "Nieznany wykonawca" : str(row[1]).trim();
            String title = blank(str(row[2])) ? "Bez tytułu" : str(row[2]).trim();
            String version = blank(str(row[3])) ? "" : " (" + str(row[3]).trim() + ")";
            out.append("<li><a href=\"/track/").append(row[0]).append("\">").append(esc(artist + " – " + title + version)).append("</a></li>");
        }
        return out.append("</ul>").toString();
    }

    private static String genreLinks() {
        StringBuilder out = new StringBuilder("<ul>");
        for (String[] g : GENRES) {
            out.append("<li><a href=\"/tracklist/").append(g[1]).append("\">").append(esc(g[2])).append("</a></li>");
        }
        out.append("<li><a href=\"/sety\">Sety i mixy</a></li>");
        return out.append("</ul>").toString();
    }

    private static Map<String, Object> breadcrumbs(String[][] trail) {
        return breadcrumbs("VIXA", trail);
    }

    private static Map<String, Object> breadcrumbs(String root, String[][] trail) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("@type", "ListItem", "position", 1, "name", root, "item", SITE + "/"));
        for (int i = 0; i < trail.length; i++) {
            items.add(Map.of("@type", "ListItem", "position", i + 2, "name", trail[i][0], "item", SITE + trail[i][1]));
        }
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "BreadcrumbList");
        ld.put("itemListElement", items);
        return ld;
    }

    private static String[] genreOf(String raw) {
        String key = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return GENRES.stream().filter(g -> g[0].equals(key)).findFirst()
                .orElse(new String[]{key, key.toLowerCase(Locale.ROOT).replace('_', '-'), "Muzyka", ""});
    }

    private static String setLabel(String category) {
        return SetCategory.parse(category).map(c -> SET_LABELS.get(c)[0]).orElse("Set");
    }

    private static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int space = text.lastIndexOf(' ', max - 1);
        return text.substring(0, space > 80 ? space : max - 1) + "…";
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    static String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
