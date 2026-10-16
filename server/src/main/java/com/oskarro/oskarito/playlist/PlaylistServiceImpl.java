package com.oskarro.oskarito.playlist;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final PlaylistEntryRepository entryRepository;
    private final UserRepository userRepository;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
                               TrackRepository trackRepository,
                               PlaylistEntryRepository entryRepository,
                               UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PlaylistView create(PlaylistDraft draft) {
        String name = draft == null || draft.name() == null ? "" : draft.name().trim();
        if (name.length() < 2 || name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nazwa składanki ma mieć od 2 do 80 znaków");
        }
        String description = draft.description() == null ? "" : draft.description().trim();
        if (description.length() > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opis może mieć najwyżej 280 znaków");
        }
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setDescription(description.isEmpty() ? null : description);
        playlist.setUser(currentUser());
        return toView(playlistRepository.save(playlist));
    }

    @Override
    public PlaylistView view(Integer id) {
        return toView(require(id));
    }

    @Override
    public List<PlaylistView> listAll() {
        return playlistRepository.findAll().stream().map(this::toView).toList();
    }

    @Override
    public List<PlaylistView> listByUsername(String username) {
        return playlistRepository.findAllByUserUsername(username).stream().map(this::toView).toList();
    }

    @Override
    public void deleteOwned(Integer id) {
        Playlist playlist = require(id);
        User owner = playlist.getUser();
        User user = currentUser();
        if (owner == null || !owner.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Składankę może usunąć tylko jej autor");
        }
        trackRepository.findTracksByPlaylistId(id).forEach(trackRepository::delete);
        entryRepository.deleteByPlaylist_Id(id);
        playlistRepository.deleteById(id);
    }

    @Override
    public List<Track> tracks(Integer playlistId) {
        require(playlistId);
        List<Track> tracks = new ArrayList<>(trackRepository.findTracksByPlaylistId(playlistId));
        Set<Integer> seen = new HashSet<>();
        for (Track track : tracks) {
            seen.add(track.getId());
        }
        for (PlaylistEntry entry : entryRepository.findByPlaylist_IdOrderByIdAsc(playlistId)) {
            Track track = entry.getTrack();
            if (track != null && track.getId() != null && seen.add(track.getId())) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    @Override
    public String pin(Integer playlistId, Integer trackId) {
        Playlist playlist = require(playlistId);
        User owner = playlist.getUser();
        User user = currentUser();
        if (owner == null || !owner.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Do składanki zapisuje tylko jej autor");
        }
        if (trackId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brakuje utworu");
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        if (track.getUrl() == null || track.getUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten utwór nie ma pliku");
        }
        boolean alreadyOwned = track.getPlaylist() != null && playlistId.equals(track.getPlaylist().getId());
        if (!alreadyOwned && !entryRepository.existsByPlaylist_IdAndTrack_Id(playlistId, trackId)) {
            PlaylistEntry entry = new PlaylistEntry();
            entry.setPlaylist(playlist);
            entry.setTrack(track);
            entryRepository.save(entry);
        }
        return playlist.getName();
    }

    private Playlist require(Integer id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiej składanki"));
    }

    private PlaylistView toView(Playlist playlist) {
        String owner = playlist.getUser() == null ? "" : playlist.getUser().getUsername();
        int tracks = trackRepository.findTracksByPlaylistId(playlist.getId()).size()
                + (int) entryRepository.countByPlaylist_Id(playlist.getId());
        return new PlaylistView(playlist.getId(), playlist.getName(), playlist.getDescription(), owner, tracks);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się, żeby założyć składankę");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się, żeby założyć składankę"));
    }

    @Override
    public List<Playlist> getAllPlaylist() {
        return playlistRepository.findAll();
    }

    @Override
    public Optional<Playlist> findPlaylistById(Integer id) {
        return playlistRepository.findById(id);
    }

    @Override
    public Optional<Playlist> findPlaylistByName(String name) {
        return playlistRepository.findByName(name);
    }

    @Override
    public Playlist updatePlaylist(Playlist playlist, Integer id) {
        return null;
    }

    @Override
    public void deletePlaylistById(Integer id) {
        playlistRepository.deleteById(id);
    }

    @Override
    public List<Playlist> findAllPlaylistByUsername(String username) {
        return playlistRepository.findAllByUserUsername(username);
    }

}
