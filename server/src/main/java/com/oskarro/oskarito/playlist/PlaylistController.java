package com.oskarro.oskarito.playlist;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackSocialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/playlist")
@CrossOrigin(origins = "http://localhost:4200")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final TrackSocialService trackSocialService;

    public PlaylistController(PlaylistService playlistService, TrackSocialService trackSocialService) {
        this.playlistService = playlistService;
        this.trackSocialService = trackSocialService;
    }

    @GetMapping(value = "/findAll")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<PlaylistView> findAll() {
        return playlistService.listAll();
    }

    @GetMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    PlaylistView getPlaylistById(@PathVariable Integer id) {
        return playlistService.view(id);
    }

    @GetMapping(value = "/{id}/tracks")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> getAllTracksFromPlaylist(@PathVariable Integer id) {
        return trackSocialService.decorateAll(playlistService.tracks(id));
    }

    @PostMapping(value = "/{id}/pin/{trackId}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public Map<String, String> pin(@PathVariable Integer id, @PathVariable Integer trackId) {
        return Map.of("name", playlistService.pin(id, trackId));
    }

    @DeleteMapping(value = "/{id}/pin/{trackId}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void unpin(@PathVariable Integer id, @PathVariable Integer trackId) {
        playlistService.unpin(id, trackId);
    }

    /** Playlists the signed-in user may play in the radio. */
    @GetMapping(value = "/playable")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public List<PlaylistView> playable() {
        return playlistService.playable();
    }

    /** Whether the user may browse everyone's playlists, and from which rank. */
    @GetMapping(value = "/access")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public Map<String, Object> access() {
        return playlistService.access();
    }

    @PutMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public PlaylistView update(@PathVariable Integer id, @RequestBody PlaylistDraft draft) {
        return playlistService.update(id, draft);
    }

    @PostMapping(value = "/add")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public PlaylistView addPlaylist(@RequestBody PlaylistDraft draft) {
        return playlistService.create(draft);
    }

    @DeleteMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void delete(@PathVariable Integer id) {
        playlistService.deleteOwned(id);
    }

    @GetMapping(value = "/all/{username}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public List<PlaylistView> getAllPlaylistByUserName(@PathVariable String username) {
        return playlistService.listByUsername(username);
    }
}
