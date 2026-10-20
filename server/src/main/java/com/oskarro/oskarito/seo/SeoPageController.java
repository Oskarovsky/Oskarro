package com.oskarro.oskarito.seo;

import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.track.CoverStorage;
import com.oskarro.oskarito.track.SetCategory;
import com.oskarro.oskarito.track.Track;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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

    static final String SITE = "https://oskarro.com";
    private static final String LOGO = SITE + "/assets/logo.jpg";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Warsaw"));
    private static final String HOME_TITLE = "Muzyka klubowa, vixa i disco polo za darmo — słuchaj z nami | Oskarro";
    private static final String HOME_DESCRIPTION = "Oskarro.com — słuchaj z nami muzyki klubowej za darmo: club, vixa, trance, hands up i disco polo. "
            + "Oceniaj, komentuj i dodawaj swoje numery.";

    /** Public genres in site order: enum name, URL slug, label, one-line description. */
    static final List<String[]> GENRES = List.of(
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
            "^/(login|register|przypomnij|haslo|aktywuj|logowanie/.*|profile(/.*)?|admin(/.*)?|mod(/.*)?|user(/.*)?|toplist(/.*)?|szukaj|zglos-naruszenie)$|.*/(add|edit)$");
    private static final Pattern TRACK = Pattern.compile("^/track/(\\d{1,9})$");
    private static final Pattern GENRE = Pattern.compile("^/tracklist/([a-z0-9-]{1,30})$");
    private static final Pattern SETS = Pattern.compile("^/sety(?:/([a-z0-9-]{1,30}))?$");
    private static final Pattern PLAYLIST = Pattern.compile("^/playlist/(\\d{1,9})/details$");

    private static final Map<String, String[]> STATIC_PAGES = Map.of(
            "/ranking", new String[]{"Ranking użytkowników i najlepsze numery | Oskarro", "Kto dodaje najwięcej muzyki klubowej na Oskarro i które numery biorą najwięcej głosów.", "Ranking"},
            "/radio", new String[]{"Radio z muzyką klubową, vixą i disco polo | Oskarro", "Włącz radio Oskarro i słuchaj z nami muzyki klubowej na imprezę — wybierz gatunki albo swoją składankę. Kolejny numer startuje sam.", "Radio Oskarro"},
            "/video", new String[]{"Sety wideo na imprezę | Oskarro", "Sety Oskarro na imprezę, do auta i na parkiet. Mixy klubowe z kanału.", "Sety wideo"},
            "/playlist/all", new String[]{"Playlisty i składanki na imprezę | Oskarro", "Składanki na imprezę, do auta i na siłownię, ułożone przez ludzi z Oskarro.", "Playlisty"},
            "/regulamin", new String[]{"Regulamin | Oskarro", "Regulamin serwisu Oskarro.com.", "Regulamin"},
            "/polityka-prywatnosci", new String[]{"Polityka prywatności | Oskarro", "Jak Oskarro.com przetwarza dane osobowe.", "Polityka prywatności"},
            "/cookies", new String[]{"Pliki cookies | Oskarro", "Jakich plików cookies używa Oskarro.com.", "Pliki cookies"});

    @PersistenceContext
    private EntityManager em;

    private final CoverStorage covers;
    private final ObjectMapper json;
    private final Path indexFile;
    private volatile String cachedIndex;
    private volatile FileTime cachedAt;

    public SeoPageController(CoverStorage covers, ObjectMapper json,
                             @Value("${oskarro.web.index:/var/www/oskarro/index.html}") String indexFile) {
        this.covers = covers;
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
        Page page = build(path);
        return ResponseEntity.status(page.status())
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(render(page));
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
        String[] known = STATIC_PAGES.get(path);
        if (known != null) {
            return new Page(200, known[0], known[1], path, true, LOGO, "website", null,
                    "<h1>" + esc(known[2]) + "</h1><p>" + esc(known[1]) + "</p>" + genreLinks());
        }
        if (PRIVATE.matcher(path).matches()) {
            return new Page(200, "Oskarro", HOME_DESCRIPTION, path, false, LOGO, "website", null, "");
        }
        return notFound(path, "Nie ma takiej strony");
    }

    private Page home() {
        StringBuilder body = new StringBuilder("<h1>Muzyka klubowa, vixa i disco polo — słuchaj z nami</h1><p>")
                .append(esc(HOME_DESCRIPTION)).append("</p><h2>Gatunki</h2>").append(genreLinks())
                .append("<h2>Ostatnio dodane</h2>").append(trackList(latest(null, false, 30)))
                .append("<h2>Sety i mixy</h2>").append(trackList(latest(null, true, 10)))
                .append("<p><a href=\"/sety\">Wszystkie sety</a> · <a href=\"/radio\">Radio</a> · <a href=\"/ranking\">Ranking</a> · <a href=\"/playlist/all\">Playlisty</a></p>");
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "WebSite");
        ld.put("name", "Oskarro");
        ld.put("url", SITE + "/");
        ld.put("description", HOME_DESCRIPTION);
        ld.put("inLanguage", "pl");
        ld.put("potentialAction", Map.of("@type", "SearchAction",
                "target", SITE + "/szukaj?q={search_term_string}", "query-input", "required name=search_term_string"));
        return new Page(200, HOME_TITLE, HOME_DESCRIPTION, "/", true, LOGO, "website", ld, body.toString());
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
        String description = cut((set ? "Posłuchaj seta za darmo: " : "Posłuchaj za darmo: ") + full + ". "
                + section + " na Oskarro" + (added.isEmpty() ? "" : ", dodane " + added)
                + ". Oceniaj, komentuj i zapisz do swojej składanki.", 158);
        String image = covers.exists(id) ? SITE + "/api/tracks/" + id + "/cover" : LOGO;
        String author = track.getAuthor() == null ? null : track.getAuthor().getUsername();

        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">Oskarro</a> › ");
        if (set) {
            body.append("<a href=\"/sety\">Sety</a>");
        } else {
            body.append("<a href=\"/tracklist/").append(genre[1]).append("\">").append(esc(genre[2])).append("</a>");
        }
        body.append("</nav><h1>").append(esc(title)).append(esc(version)).append("</h1><p><strong>").append(esc(artist)).append("</strong></p><p>")
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
        return new Page(200, full + " | Oskarro", description, path, true, image, "music.song",
                List.of(recording, crumbs), body.toString());
    }

    private Page genre(String slug) {
        String[] genre = GENRES.stream().filter(g -> g[1].equals(slug)).findFirst().orElse(null);
        if (genre == null) {
            return notFound("/tracklist/" + slug, "Nie ma takiego gatunku");
        }
        String path = "/tracklist/" + slug;
        String heading = genre[0].equals("CLUB") ? "Muzyka klubowa — Club" : genre[2] + " na imprezę";
        String title = genre[0].equals("CLUB") ? "Muzyka klubowa (Club) — słuchaj za darmo | Oskarro"
                : genre[2] + " na imprezę — słuchaj za darmo | Oskarro";
        String body = "<nav aria-label=\"Okruszki\"><a href=\"/\">Oskarro</a> › " + esc(genre[2]) + "</nav><h1>" + esc(heading) + "</h1><p>"
                + esc(genre[3]) + "</p><h2>Najnowsze: " + esc(genre[2]) + "</h2>" + trackList(latest(genre[0], false, 40))
                + "<h2>Inne gatunki</h2>" + genreLinks();
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("@context", "https://schema.org");
        page.put("@type", "CollectionPage");
        page.put("name", heading);
        page.put("description", genre[3]);
        page.put("url", SITE + path);
        page.put("inLanguage", "pl");
        page.put("isPartOf", Map.of("@type", "WebSite", "name", "Oskarro", "url", SITE + "/"));
        return new Page(200, title, genre[3], path, true, LOGO, "website",
                List.of(page, breadcrumbs(new String[][]{{genre[2], path}})), body);
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
                : SET_LABELS.get(category)[1] + " Słuchaj z nami za darmo na Oskarro.";
        String query = "select t.id, t.artist, t.title, t.version from Track t where t.kind = 'set' and t.url is not null and t.url <> ''"
                + (category == null ? "" : " and t.setCategory = :category") + " order by t.id desc";
        var q = em.createQuery(query, Object[].class).setMaxResults(40);
        if (category != null) {
            q.setParameter("category", category.name());
        }
        StringBuilder body = new StringBuilder("<nav aria-label=\"Okruszki\"><a href=\"/\">Oskarro</a> › ")
                .append(category == null ? "Sety" : "<a href=\"/sety\">Sety</a> › " + esc(heading))
                .append("</nav><h1>").append(esc(heading)).append("</h1><p>").append(esc(description)).append("</p><ul>");
        for (SetCategory c : SetCategory.values()) {
            body.append("<li><a href=\"/sety/").append(c.slug()).append("\">").append(esc(SET_LABELS.get(c)[0])).append("</a></li>");
        }
        body.append("</ul>").append(trackList(q.getResultList()));
        return new Page(200, heading + " — słuchaj za darmo | Oskarro", description, path, true, LOGO, "website", null, body.toString());
    }

    private Page playlist(Integer id) {
        Playlist playlist = em.find(Playlist.class, id);
        if (playlist == null || Boolean.TRUE.equals(playlist.getPrivatePlaylist())) {
            return notFound("/playlist/" + id + "/details", "Nie ma takiej składanki");
        }
        String path = "/playlist/" + id + "/details";
        String name = blank(playlist.getName()) ? "Składanka" : playlist.getName().trim();
        String owner = playlist.getUser() == null ? "Oskarro" : playlist.getUser().getUsername();
        List<Object[]> tracks = em.createQuery("select t.id, t.artist, t.title, t.version from PlaylistEntry e join e.track t"
                + " where e.playlist.id = :id order by e.id", Object[].class).setParameter("id", id).setMaxResults(100).getResultList();
        String description = cut(blank(playlist.getDescription())
                ? "Składanka „" + name + "” od " + owner + " na Oskarro — " + tracks.size() + " numerów do posłuchania za darmo."
                : playlist.getDescription().trim() + " — składanka od " + owner + " na Oskarro.", 158);
        String body = "<nav aria-label=\"Okruszki\"><a href=\"/\">Oskarro</a> › <a href=\"/playlist/all\">Playlisty</a></nav><h1>" + esc(name)
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
        return new Page(200, name + " — składanka od " + owner + " | Oskarro", description, path, true, LOGO, "music.playlist", ld, body);
    }

    private Page notFound(String path, String message) {
        return new Page(404, message + " | Oskarro", message + ".", path, false, LOGO, "website", null,
                "<h1>" + esc(message) + "</h1><p><a href=\"/\">Wróć na stronę główną Oskarro</a></p>" + genreLinks());
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
                .append("  <meta property=\"og:site_name\" content=\"Oskarro\">\n")
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
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("@type", "ListItem", "position", 1, "name", "Oskarro", "item", SITE + "/"));
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
