package com.oskarro.oskarito.track;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
@RestController
@RequestMapping(value = "/api/tracks")
@CrossOrigin(origins = "http://localhost:4200")
public class TrackController {

    private final TrackService trackService;
    private final com.oskarro.oskarito.track.TrackRepository trackRepository;
    private final TrackSocialService trackSocialService;
    private final HostujeAudioResolver hostujeAudioResolver;
    private final SoundfilesAudioResolver soundfilesAudioResolver;
    private final WaveformService waveformService;
    private final TrackFileService trackFileService;
    private final TrackQueryService trackQueryService;

    public TrackController(TrackService trackService,
                           com.oskarro.oskarito.track.TrackRepository trackRepository,
                           TrackSocialService trackSocialService,
                           HostujeAudioResolver hostujeAudioResolver,
                           SoundfilesAudioResolver soundfilesAudioResolver,
                           WaveformService waveformService,
                           TrackFileService trackFileService,
                           TrackQueryService trackQueryService) {
        this.trackService = trackService;
        this.trackRepository = trackRepository;
        this.trackSocialService = trackSocialService;
        this.hostujeAudioResolver = hostujeAudioResolver;
        this.soundfilesAudioResolver = soundfilesAudioResolver;
        this.waveformService = waveformService;
        this.trackFileService = trackFileService;
        this.trackQueryService = trackQueryService;
    }

    @GetMapping(value = "/{id}/file")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    TrackFileView file(@PathVariable Integer id) {
        return trackFileService.facts(id);
    }

    @GetMapping(value = "/play/{hash}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    PlaySource play(@PathVariable String hash) {
        return new PlaySource(hostujeAudioResolver.resolve(hash));
    }

    @GetMapping(value = "/play/{host}/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    PlaySource playFrom(@PathVariable String host, @PathVariable String id) {
        if ("hostuje".equals(host)) {
            return new PlaySource(hostujeAudioResolver.resolve(id));
        }
        if ("soundfiles".equals(host)) {
            return new PlaySource(soundfilesAudioResolver.resolve(id));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznane źródło pliku");
    }

    @GetMapping(value = "/peaks/{host}/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Waveform peaks(@PathVariable String host, @PathVariable String id) {
        return waveformService.peaks(host, id);
    }

    @GetMapping(value = "/findAll")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> findAll() {
        return trackSocialService.decorateAll(trackService.findAll());
    }

    @GetMapping(value = "/id/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Optional<Track> findById(@PathVariable Integer id) {
        return trackService.findById(id).map(trackSocialService::decorate);
    }

    @PostMapping(value = "/add")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Track addTrack(@RequestBody Track track, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are a problem with binding");
        }
        requirePlayableLink(track);
        // Only content fields are copied, so the request cannot overwrite an existing track (id) or forge author/points.
        Track trackAdded = Track.builder()
                .artist(track.getArtist())
                .title(track.getTitle())
                .version(track.getVersion())
                .url(track.getUrl())
                .genre(track.getGenre())
                .build();
        return trackService.saveTrack(trackAdded);
    }

    @GetMapping(value = "/genre/{genre}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> findByGenre(@PathVariable String genre,
                            @RequestParam(defaultValue = "new") String sort) {
        return trackSocialService.listByGenre(genre, sort);
    }

    /** Everything the home page needs in one small response (instead of the whole catalogue). */
    @GetMapping(value = "/home")
    TrackQueryService.HomeFeed home() {
        return trackQueryService.home();
    }

    @GetMapping(value = "/search")
    List<Track> search(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "100") int limit) {
        return trackQueryService.search(q, limit);
    }

    @GetMapping(value = "/sets")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> sets(@RequestParam(defaultValue = "") String category,
                     @RequestParam(defaultValue = "new") String sort) {
        return trackSocialService.listSets(category, sort);
    }

    @PostMapping(value = "/share")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Track share(@RequestBody Track track) {
        return trackSocialService.share(track);
    }

    @PutMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Track update(@PathVariable Integer id, @RequestBody Track track) {
        return trackSocialService.update(id, track);
    }

    @GetMapping(value = "/{id}/comments")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<CommentView> comments(@PathVariable Integer id) {
        return trackSocialService.comments(id);
    }

    @PostMapping(value = "/{id}/comments")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    CommentView addComment(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        return trackSocialService.addComment(id, body.get("text"));
    }

    @PostMapping(value = "/{id}/view")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    void view(@PathVariable Integer id, HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        trackSocialService.recordView(id, realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp);
    }

    /** Existing tracks that look like the one being added, so nobody shares the same track twice. */
    @GetMapping(value = "/similar")
    List<SimilarTrack> similar(@RequestParam(required = false) String artist,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) String url,
                               @RequestParam(required = false) Integer except) {
        return trackQueryService.similar(artist, title, url, except, 5);
    }

    @GetMapping(value = "/mine")
    List<Track> mine() {
        return trackSocialService.mine();
    }

    @GetMapping(value = "/liked")
    List<Track> liked() {
        return trackSocialService.liked();
    }

    @GetMapping(value = "/my-comments")
    List<MyCommentView> myComments() {
        return trackSocialService.myComments();
    }

    @PostMapping(value = "/{id}/download")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    DownloadView download(@PathVariable Integer id) {
        return trackSocialService.download(id);
    }

    @GetMapping(value = "/{id}/likes")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    LikeState likes(@PathVariable Integer id) {
        return trackSocialService.likes(id);
    }

    @PostMapping(value = "/{id}/vote")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    LikeState vote(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer value = body.get("vote");
        return trackSocialService.vote(id, value == null ? 0 : value);
    }

    @GetMapping(value = "/{id}/card", produces = MediaType.TEXT_HTML_VALUE)
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    String card(@PathVariable Integer id) {
        Track track = trackService.findById(id)
                .map(trackSocialService::decorate)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        String name = blank(track.getTitle()) ? "Utwór" : track.getTitle().trim();
        String artist = blank(track.getArtist()) ? "Nieznany wykonawca" : track.getArtist().trim();
        String heading = name + " — " + artist;
        String description = heading + ". " + genreWords(track.getGenre()) + " na VIXA, muzyka na imprezę.";
        String page = "https://oskarro.com/track/" + id;
        String image = Boolean.TRUE.equals(track.getHasCover())
                ? "https://oskarro.com/api/tracks/" + id + "/cover"
                : "https://oskarro.com/assets/logo.jpg";
        return """
                <!doctype html>
                <html lang="pl"><head>
                <meta charset="utf-8">
                <title>%s | VIXA</title>
                <meta name="description" content="%s">
                <link rel="canonical" href="%s">
                <meta property="og:type" content="music.song">
                <meta property="og:site_name" content="VIXA">
                <meta property="og:locale" content="pl_PL">
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:image" content="%s">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:title" content="%s">
                <meta name="twitter:description" content="%s">
                <meta name="twitter:image" content="%s">
                </head><body>
                <p><a href="%s">%s</a></p>
                </body></html>
                """.formatted(esc(heading), esc(description), page, esc(heading), esc(description), page, image,
                esc(heading), esc(description), image, page, esc(heading));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String genreWords(String genre) {
        if (blank(genre)) {
            return "Muzyka";
        }
        String[] parts = genre.trim().toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static String esc(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @GetMapping(value = "/{id}/cover", produces = MediaType.IMAGE_JPEG_VALUE)
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    ResponseEntity<byte[]> cover(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(trackSocialService.cover(id));
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Map<String, Boolean> uploadCover(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        trackSocialService.saveCover(id, file);
        return Map.of("hasCover", true);
    }

    @PostMapping(value = "/addToRanking")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    void addTrackToRanking (@RequestBody Track track, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Track has errors - it cannot by send");
        }
        requirePlayableLink(track);
        Track trackAdded = Track.builder()
                .artist(track.getArtist())
                .title(track.getTitle())
                .version(track.getVersion())
                .url(track.getUrl())
                .points(track.getPoints())
                .genre(track.getGenre())
                .build();
        trackService.saveTrack(trackAdded);
    }

    private void requirePlayableLink(Track track) {
        String url = track.getUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        String canonical = TrackSocialService.canonical(url);
        if (canonical != null) {
            track.setUrl(canonical);
            return;
        }
        if (track.getId() != null) {
            Optional<Track> existing = trackRepository.findById(track.getId());
            if (existing.isPresent() && url.trim().equals(existing.get().getUrl() == null ? "" : existing.get().getUrl().trim())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj link z hostuje.net albo soundfiles.eu");
    }

    @DeleteMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void delete(@PathVariable Integer id) {
        trackSocialService.delete(id);
    }

}
