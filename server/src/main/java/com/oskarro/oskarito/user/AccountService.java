package com.oskarro.oskarito.user;

import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.playlist.PlaylistEntryRepository;
import com.oskarro.oskarito.playlist.PlaylistRepository;
import com.oskarro.oskarito.storage.ImageRepository;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackCommentRepository;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackSocialService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GDPR self-service: download everything we hold about you, or delete the account with all of it. */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final TrackRepository tracks;
    private final TrackSocialService social;
    private final TrackCommentRepository comments;
    private final TrackLikeRepository votes;
    private final PlaylistRepository playlists;
    private final PlaylistEntryRepository entries;
    private final ImageRepository images;
    private final AccountActivationRepository activations;
    private final PasswordResetRepository resets;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, TrackRepository tracks, TrackSocialService social,
                          TrackCommentRepository comments, TrackLikeRepository votes, PlaylistRepository playlists,
                          PlaylistEntryRepository entries, ImageRepository images,
                          AccountActivationRepository activations, PasswordResetRepository resets,
                          PasswordEncoder encoder) {
        this.users = users;
        this.tracks = tracks;
        this.social = social;
        this.comments = comments;
        this.votes = votes;
        this.playlists = playlists;
        this.entries = entries;
        this.images = images;
        this.activations = activations;
        this.resets = resets;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export() {
        User user = currentUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now().toString());
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("username", user.getUsername());
        account.put("email", user.getEmail());
        account.put("createdAt", iso(user.getCreatedAt()));
        account.put("termsAcceptedAt", iso(user.getTermsAcceptedAt()));
        account.put("lastLoginAt", iso(user.getLastLoginAt()));
        account.put("signInMethod", user.getAuthProvider() == null ? "password" : user.getAuthProvider());
        account.put("roles", user.getRoles().stream().map(role -> role.getName().name()).sorted().toList());
        account.put("bio", user.getBio());
        account.put("firstName", user.getFirstName());
        account.put("lastName", user.getLastName());
        account.put("website", user.getWebsite());
        account.put("favoriteGenres", user.getFavoriteGenres());
        account.put("hasAvatar", images.existsByUserId(user.getId()));
        data.put("account", account);
        data.put("tracks", tracks.findByAuthor_IdOrderByIdDesc(user.getId()).stream().map(track -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", track.getId());
            row.put("type", "set".equals(track.getKind()) ? "set" : "track");
            row.put("artist", track.getArtist());
            row.put("title", track.getTitle());
            row.put("version", track.getVersion());
            row.put("genre", track.getGenre());
            row.put("setCategory", track.getSetCategory());
            row.put("eventName", track.getEventName());
            row.put("tracklist", track.getTracklist());
            row.put("url", track.getUrl());
            row.put("createdAt", iso(track.getCreatedAt()));
            return row;
        }).toList());
        data.put("comments", comments.findByAuthor_IdOrderByCreatedAtDesc(user.getId()).stream().map(comment -> Map.of(
                "trackId", comment.getTrack().getId(),
                "track", String.valueOf(comment.getTrack().getTitle()),
                "text", comment.getText(),
                "createdAt", String.valueOf(iso(comment.getCreatedAt()))
        )).toList());
        data.put("votes", votes.findByUser_Id(user.getId()).stream().map(vote -> Map.of(
                "trackId", vote.getTrack().getId(),
                "track", String.valueOf(vote.getTrack().getTitle()),
                "vote", vote.getVote() == null || vote.getVote() >= 0 ? 1 : -1,
                "createdAt", String.valueOf(iso(vote.getCreatedAt()))
        )).toList());
        data.put("playlists", playlists.findAllByUserUsername(user.getUsername()).stream().map(playlist -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", playlist.getName());
            row.put("description", playlist.getDescription());
            row.put("tracks", entries.findByPlaylist_IdOrderByIdAsc(playlist.getId()).stream()
                    .map(entry -> entry.getTrack().getArtist() + " – " + entry.getTrack().getTitle()).toList());
            return row;
        }).toList());
        return data;
    }

    /**
     * Deletes the signed-in account and everything it created. Password accounts confirm with the password,
     * Google/Facebook accounts by typing their username.
     */
    public void delete(String confirmation) {
        User user = currentUser();
        String given = confirmation == null ? "" : confirmation;
        boolean external = user.getAuthProvider() != null && !"local".equals(user.getAuthProvider());
        boolean confirmed = external ? given.trim().equals(user.getUsername()) : encoder.matches(given, user.getPassword());
        if (!confirmed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    external ? "Wpisz swoją nazwę użytkownika, żeby potwierdzić" : "Hasło się nie zgadza");
        }
        boolean admin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (admin && users.findAll().stream().filter(other -> other.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN)).count() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To jedyne konto administratora — najpierw nadaj tę rolę komuś innemu");
        }
        for (Track track : tracks.findByAuthor_IdOrderByIdDesc(user.getId())) {
            social.delete(track.getId());
        }
        comments.deleteByAuthorId(user.getId());
        votes.deleteByVoterId(user.getId());
        for (Playlist playlist : playlists.findAllByUserUsername(user.getUsername())) {
            entries.deleteByPlaylist_Id(playlist.getId());
            for (Track legacy : tracks.findTracksByPlaylistId(playlist.getId())) {
                legacy.setPlaylist(null);
                tracks.save(legacy);
            }
            playlists.deleteById(playlist.getId());
        }
        if (images.existsByUserId(user.getId())) {
            images.deleteByUserId(user.getId());
        }
        activations.deleteByUserId(user.getId());
        resets.deleteByUserId(user.getId());
        user.getRoles().clear();
        users.delete(user);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się"));
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
