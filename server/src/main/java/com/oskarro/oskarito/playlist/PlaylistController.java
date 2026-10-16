package com.oskarro.oskarito.playlist;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/playlist")
@CrossOrigin(origins = "http://localhost:4200")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final TrackService trackService;

    public PlaylistController(PlaylistService playlistService, TrackService trackService) {
        this.playlistService = playlistService;
        this.trackService = trackService;
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
        return trackService.findAllTracksFromPlaylist(id);
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
