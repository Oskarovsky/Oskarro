package com.oskarro.oskarito.seo;

import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.SetCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Live sitemap (nginx serves it at /sitemap.xml): pages, genres, set categories and every track/set. */
@RestController
public class SitemapController {

    private static final String SITE = "https://oskarro.com";
    /** Genres shown on the site (the enum also keeps legacy values). */
    private static final List<Genre> PUBLIC = List.of(Genre.CLUB, Genre.DANCE, Genre.RETRO, Genre.HOUSE, Genre.VIXA,
            Genre.BASS_HOUSE, Genre.TECHNO, Genre.BIG_ROOM, Genre.TRANCE, Genre.ELECTRO, Genre.DISCO_POLO, Genre.HANDS_UP, Genre.POP);

    @PersistenceContext
    private EntityManager em;

    @GetMapping(value = "/api/seo/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        String today = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        url(xml, "/", today, "daily", "1.0");
        for (String page : List.of("/sety", "/ranking", "/radio", "/video", "/playlist/all")) {
            url(xml, page, today, "daily", "0.8");
        }
        for (Genre genre : PUBLIC) {
            url(xml, "/tracklist/" + genre.name().toLowerCase(Locale.ROOT).replace('_', '-'), today, "daily", "0.8");
        }
        for (SetCategory category : SetCategory.values()) {
            url(xml, "/sety/" + category.slug(), today, "daily", "0.7");
        }
        for (Object[] row : em.createQuery("select t.id, t.createdAt from Track t where t.genre is not null and t.genre <> ''"
                + " and t.url is not null and t.url <> '' order by t.id desc", Object[].class).getResultList()) {
            Instant created = (Instant) row[1];
            url(xml, "/track/" + row[0], created == null ? null : created.atOffset(ZoneOffset.UTC).toLocalDate().toString(), "weekly", "0.6");
        }
        for (String page : List.of("/regulamin", "/polityka-prywatnosci", "/cookies")) {
            url(xml, page, null, "yearly", "0.2");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic()).body(xml.toString());
    }

    private static void url(StringBuilder xml, String path, String lastmod, String freq, String priority) {
        xml.append("  <url><loc>").append(SITE).append(path).append("</loc>");
        if (lastmod != null) {
            xml.append("<lastmod>").append(lastmod).append("</lastmod>");
        }
        xml.append("<changefreq>").append(freq).append("</changefreq><priority>").append(priority).append("</priority></url>\n");
    }
}
