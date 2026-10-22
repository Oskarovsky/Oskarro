package com.oskarro.oskarito.like;

import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.playlist.PlaylistRepository;
import com.oskarro.oskarito.track.DownloadAllowanceService;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ContentLikeService {

    private static final Pattern YOUTUBE_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private final ContentLikeRepository likes;
    private final PlaylistRepository playlists;
    private final UserRepository users;
    private final DownloadAllowanceService downloads;

    public ContentLikeService(ContentLikeRepository likes, PlaylistRepository playlists,
                              UserRepository users, DownloadAllowanceService downloads) {
        this.likes = likes;
        this.playlists = playlists;
        this.users = users;
        this.downloads = downloads;
    }

    public LikeView state(String kind, String key) {
        Optional<User> viewer = currentUser();
        ownerOf(kind, key, viewer.orElse(null));
        boolean liked = viewer.flatMap(user -> likes.findByUserIdAndKindAndItemKey(user.getId(), kind, key)).isPresent();
        return new LikeView(likes.countByKindAndItemKey(kind, key), liked, downloads.credits(viewer.orElse(null)));
    }

    /** Like or take the like back. The first like of an item gives download credits, unless it is your own playlist. */
    public LikeView toggle(String kind, String key) {
        User user = currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się, żeby polubić"));
        Integer owner = ownerOf(kind, key, user);
        Optional<ContentLike> existing = likes.findByUserIdAndKindAndItemKey(user.getId(), kind, key);
        if (existing.isPresent()) {
            likes.delete(existing.get());
        } else {
            ContentLike like = new ContentLike();
            like.setUserId(user.getId());
            like.setKind(kind);
            like.setItemKey(key);
            like.setCreatedAt(Instant.now());
            try {
                likes.saveAndFlush(like);
            } catch (DataIntegrityViolationException ex) {
                // A double click raced the first one; the like is already there.
            }
            if (!Objects.equals(owner, user.getId())) {
                downloads.grant(user, DownloadAllowanceService.Activity.LIKE, kind + ":" + key);
            }
        }
        return new LikeView(likes.countByKindAndItemKey(kind, key),
                likes.findByUserIdAndKindAndItemKey(user.getId(), kind, key).isPresent(), downloads.credits(user));
    }

    /** Checks the item exists and may be seen, and returns its owner's id (null for YouTube sets). */
    private Integer ownerOf(String kind, String key, User viewer) {
        if ("video".equals(kind)) {
            if (key == null || !YOUTUBE_ID.matcher(key).matches()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego wideo");
            }
            return null;
        }
        if ("playlist".equals(kind)) {
            Playlist playlist;
            try {
                playlist = playlists.findById(Integer.valueOf(key)).orElse(null);
            } catch (NumberFormatException ex) {
                playlist = null;
            }
            Integer owner = playlist == null || playlist.getUser() == null ? null : playlist.getUser().getId();
            boolean hidden = playlist != null && Boolean.TRUE.equals(playlist.getPrivatePlaylist())
                    && (viewer == null || !Objects.equals(owner, viewer.getId()));
            if (playlist == null || hidden) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiej składanki");
            }
            return owner;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tego nie da się polubić");
    }

    private Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return users.findByUsername(authentication.getName());
    }
}
