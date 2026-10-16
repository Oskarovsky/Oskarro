package com.oskarro.oskarito.playlist;

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

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
                               TrackRepository trackRepository,
                               UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
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
        playlistRepository.deleteById(id);
    }

    private Playlist require(Integer id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiej składanki"));
    }

    private PlaylistView toView(Playlist playlist) {
        String owner = playlist.getUser() == null ? "" : playlist.getUser().getUsername();
        int tracks = trackRepository.findTracksByPlaylistId(playlist.getId()).size();
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
