package com.oskarro.oskarito.seo;

import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.SetCategory;
import com.oskarro.oskarito.explore.ExploreService;
import com.oskarro.oskarito.video.ChannelCatalog;
import com.oskarro.oskarito.video.ChannelVideo;
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

    private final ChannelCatalog channel;
    private final ExploreService explore;

    public SitemapController(ChannelCatalog channel, ExploreService explore) {
        this.channel = channel;
        this.explore = explore;
    }

    @GetMapping(value = "/api/seo/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        String today = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        url(xml, "/", today, "daily", "1.0");
        for (String page : List.of("/sety", "/tracklist", "/ranking", "/radio", "/video", "/playlist/all")) {
            url(xml, page, today, "daily", "0.8");
        }
        for (Genre genre : PUBLIC) {
            url(xml, "/tracklist/" + genre.name().toLowerCase(Locale.ROOT).replace('_', '-'), today, "daily", "0.8");
        }
        // Monthly "Nowości" pages, genre charts and artist pages: the landing pages people search for.
        for (Genre genre : PUBLIC) {
            String slug = genre.name().toLowerCase(Locale.ROOT).replace('_', '-');
            List<ExploreService.MonthLink> months = explore.months(slug);
            if (!months.isEmpty()) {
                url(xml, "/nowosci/" + slug, today, "weekly", "0.7");
                for (int i = 0; i < months.size(); i++) {
                    url(xml, "/nowosci/" + slug + "/" + months.get(i).month(), i == 0 ? today : null, i == 0 ? "daily" : "monthly", i == 0 ? "0.8" : "0.5");
                }
            }
            url(xml, "/ranking/" + slug, today, "daily", "0.7");
            url(xml, "/ranking/" + slug + "/miesiac", today, "daily", "0.6");
            url(xml, "/ranking/" + slug + "/ogolny", today, "weekly", "0.5");
        }
        url(xml, "/ranking/sety", today, "daily", "0.6");
        for (ExploreService.ArtistLink artist : explore.artists()) {
            url(xml, "/wykonawca/" + artist.slug(), null, "weekly", "0.5");
        }
        for (SetCategory category : SetCategory.values()) {
            url(xml, "/sety/" + category.slug(), today, "daily", "0.7");
        }
        for (Object[] row : em.createQuery("select t.id, t.createdAt from Track t where t.genre is not null and t.genre <> ''"
                + " and t.url is not null and t.url <> '' order by t.id desc", Object[].class).getResultList()) {
            Instant created = (Instant) row[1];
            url(xml, "/track/" + row[0], created == null ? null : created.atOffset(ZoneOffset.UTC).toLocalDate().toString(), "weekly", "0.6");
        }
        try {
            for (ChannelVideo video : channel.videos()) {
                url(xml, "/video/set/" + video.id(), null, "monthly", "0.5");
            }
        } catch (RuntimeException youtubeUnavailable) {
            // The sitemap stays valid without the channel sets; they come back on the next fetch.
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
