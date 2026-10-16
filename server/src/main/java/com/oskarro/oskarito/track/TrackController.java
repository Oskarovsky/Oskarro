package com.oskarro.oskarito.track;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping(value = "/api/tracks")
@CrossOrigin(origins = "http://localhost:4200")
public class TrackController {

    private static final Pattern HOSTUJE_LINK = Pattern.compile(
            "(?i)(?:src=[\"']\\s*)?(?:https?://)?(?:www\\.)?hostuje\\.net/(?:file\\.php\\?id=|player_embed\\.js\\.php\\?id=)[a-f0-9]{32}|^[a-f0-9]{32}$");

    private final TrackService trackService;
    private final com.oskarro.oskarito.track.TrackRepository trackRepository;
    private final TrackSocialService trackSocialService;
    private final HostujeAudioResolver hostujeAudioResolver;
    private final SoundfilesAudioResolver soundfilesAudioResolver;
    private final WaveformService waveformService;

    public TrackController(TrackService trackService,
                           com.oskarro.oskarito.track.TrackRepository trackRepository,
                           TrackSocialService trackSocialService,
                           HostujeAudioResolver hostujeAudioResolver,
                           SoundfilesAudioResolver soundfilesAudioResolver,
                           WaveformService waveformService) {
        this.trackService = trackService;
        this.trackRepository = trackRepository;
        this.trackSocialService = trackSocialService;
        this.hostujeAudioResolver = hostujeAudioResolver;
        this.soundfilesAudioResolver = soundfilesAudioResolver;
        this.waveformService = waveformService;
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
        requireKrakenLink(track);
        return trackService.saveTrack(track);
    }

    @GetMapping(value = "/genre/{genre}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> findByGenre(@PathVariable String genre,
                            @RequestParam(defaultValue = "score") String sort) {
        return trackSocialService.listByGenre(genre, sort);
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
        requireKrakenLink(track);
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

    private void requireKrakenLink(Track track) {
        String url = track.getUrl();
        if (url == null || url.isBlank() || HOSTUJE_LINK.matcher(url.trim()).find()) {
            return;
        }
        if (track.getId() != null) {
            Optional<Track> existing = trackRepository.findById(track.getId());
            if (existing.isPresent() && url.trim().equals(existing.get().getUrl() == null ? "" : existing.get().getUrl().trim())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Track url must be a hostuje.net link");
    }

    @DeleteMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void delete(@PathVariable Integer id) {
        this.trackRepository.deleteById(id);
    }

}
