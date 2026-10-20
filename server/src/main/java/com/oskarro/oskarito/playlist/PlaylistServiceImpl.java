package com.oskarro.oskarito.playlist;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.user.ProfileService;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final PlaylistEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
                               TrackRepository trackRepository,
                               PlaylistEntryRepository entryRepository,
                               UserRepository userRepository,
                               ProfileService profileService) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.profileService = profileService;
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
        playlist.setPrivatePlaylist(draft.privatePlaylist() != null && draft.privatePlaylist());
        return toView(playlistRepository.save(playlist));
    }

    @Override
    public PlaylistView view(Integer id) {
        return toView(requireVisible(id));
    }

    /** Everyone's playlists: a perk from rank Bywalec (or staff); others' private playlists are never listed. */
    @Override
    public List<PlaylistView> listAll() {
        User viewer = currentUserOptional().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się, żeby przeglądać składanki"));
        if (!profileService.hasPlaylistAccess(viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Wszystkie składanki są dostępne od rangi " + profileService.playlistAccessRank());
        }
        return playlistRepository.findAll().stream().filter(p -> visibleTo(p, viewer)).map(this::toView).toList();
    }

    @Override
    public List<PlaylistView> listByUsername(String username) {
        User viewer = currentUserOptional().orElse(null);
        return playlistRepository.findAllByUserUsername(username).stream()
                .filter(p -> visibleTo(p, viewer)).map(this::toView).toList();
    }

    /** What the signed-in user may play in the radio: own playlists, plus everyone's public ones with enough rank. */
    @Override
    public List<PlaylistView> playable() {
        User viewer = currentUser();
        boolean wide = profileService.hasPlaylistAccess(viewer);
        return playlistRepository.findAll().stream()
                .filter(p -> isOwner(p, viewer) || (wide && !isPrivate(p)))
                .map(this::toView)
                .filter(view -> view.tracks() > 0)
                .sorted(Comparator.comparing(PlaylistView::mine).reversed()
                        .thenComparing(PlaylistView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Map<String, Object> access() {
        User viewer = currentUserOptional().orElse(null);
        return Map.of("canBrowseAll", profileService.hasPlaylistAccess(viewer),
                "rank", profileService.playlistAccessRank(),
                "signedIn", viewer != null);
    }

    @Override
    public PlaylistView update(Integer id, PlaylistDraft draft) {
        Playlist playlist = require(id);
        if (!isOwner(playlist, currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Składankę zmienia tylko jej autor");
        }
        String name = draft == null || draft.name() == null ? "" : draft.name().trim();
        if (name.length() < 2 || name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nazwa składanki ma mieć od 2 do 80 znaków");
        }
        String description = draft.description() == null ? "" : draft.description().trim();
        if (description.length() > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opis może mieć najwyżej 280 znaków");
        }
        playlist.setName(name);
        playlist.setDescription(description.isEmpty() ? null : description);
        if (draft.privatePlaylist() != null) {
            playlist.setPrivatePlaylist(draft.privatePlaylist());
        }
        return toView(playlistRepository.save(playlist));
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
        requireVisible(playlistId);
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

    @Override
    public void unpin(Integer playlistId, Integer trackId) {
        Playlist playlist = require(playlistId);
        User owner = playlist.getUser();
        User user = currentUser();
        if (owner == null || !owner.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ze składanki usuwa tylko jej autor");
        }
        if (entryRepository.existsByPlaylist_IdAndTrack_Id(playlistId, trackId)) {
            entryRepository.deleteByPlaylist_IdAndTrack_Id(playlistId, trackId);
            return;
        }
        // Legacy tracks belong to the playlist directly; detach them instead of deleting the track.
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        if (track.getPlaylist() == null || !playlistId.equals(track.getPlaylist().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tego utworu nie ma w składance");
        }
        track.setPlaylist(null);
        trackRepository.save(track);
    }

    private Playlist require(Integer id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiej składanki"));
    }

    private PlaylistView toView(Playlist playlist) {
        String owner = playlist.getUser() == null ? "" : playlist.getUser().getUsername();
        int tracks = trackRepository.findTracksByPlaylistId(playlist.getId()).size()
                + (int) entryRepository.countByPlaylist_Id(playlist.getId());
        boolean mine = currentUserOptional().map(viewer -> isOwner(playlist, viewer)).orElse(false);
        return new PlaylistView(playlist.getId(), playlist.getName(), playlist.getDescription(), owner, tracks,
                isPrivate(playlist), mine);
    }

    /** Private playlists exist only for their owner: everyone else gets a plain 404. */
    private Playlist requireVisible(Integer id) {
        Playlist playlist = require(id);
        if (!visibleTo(playlist, currentUserOptional().orElse(null))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiej składanki");
        }
        return playlist;
    }

    private static boolean isPrivate(Playlist playlist) {
        return Boolean.TRUE.equals(playlist.getPrivatePlaylist());
    }

    private static boolean isOwner(Playlist playlist, User user) {
        return user != null && playlist.getUser() != null && playlist.getUser().getId().equals(user.getId());
    }

    private static boolean visibleTo(Playlist playlist, User viewer) {
        return !isPrivate(playlist) || isOwner(playlist, viewer);
    }

    private Optional<User> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
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
