package com.oskarro.oskarito.track;

import com.oskarro.oskarito.playlist.PlaylistEntryRepository;
import com.oskarro.oskarito.user.ProfileService;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TrackSocialService {

    private static final Duration EDIT_WINDOW = Duration.ofHours(24);

    private static final Pattern HOSTUJE_LINK = Pattern.compile(
            "(?i)(?:src=[\"']\\s*)?(?:https?://)?(?:www\\.)?hostuje\\.net/(?:file\\.php\\?id=|player_embed\\.js\\.php\\?id=)([a-f0-9]{32})|^([a-f0-9]{32})$");
    private static final Pattern SOUNDFILES_LINK = Pattern.compile(
            "(?i)(?:https?://)?(?:www\\.)?soundfiles\\.eu/([A-Za-z0-9]{12})(?:/(?:file(?:/(?:download|stream))?)?)?");
    private static final Pattern SOUNDFILES_ID = Pattern.compile("^[A-Za-z0-9]{12}$");

    private final TrackRepository trackRepository;
    private final TrackCommentRepository commentRepository;
    private final TrackLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final CoverStorage coverStorage;
    private final TrackViewRepository viewRepository;
    private final PlaylistEntryRepository entryRepository;
    private final TrackFileRepository fileRepository;
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    public TrackSocialService(TrackRepository trackRepository,
                              TrackCommentRepository commentRepository,
                              TrackLikeRepository likeRepository,
                              UserRepository userRepository,
                              ProfileService profileService,
                              CoverStorage coverStorage,
                              TrackViewRepository viewRepository,
                              PlaylistEntryRepository entryRepository,
                              TrackFileRepository fileRepository) {
        this.trackRepository = trackRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.coverStorage = coverStorage;
        this.viewRepository = viewRepository;
        this.entryRepository = entryRepository;
        this.fileRepository = fileRepository;
    }

    public List<Track> listByGenre(String genre, String sort) {
        String normalized = normalizeGenre(genre);
        List<Track> tracks = new ArrayList<>(trackRepository.findTracksByGenre(normalized));
        decorateAll(tracks);
        Comparator<Track> newest = Comparator
                .comparing(Track::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Track::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        String mode = sort == null ? "new" : sort.toLowerCase(Locale.ROOT);
        if ("score".equals(mode)) {
            tracks.sort(Comparator.comparingInt(Track::getScore).reversed().thenComparing(newest));
        } else if ("views-month".equals(mode)) {
            tracks.sort(Comparator.comparingInt(Track::getViewsMonth).reversed().thenComparing(newest));
        } else if ("views-year".equals(mode)) {
            tracks.sort(Comparator.comparingInt(Track::getViewsYear).reversed().thenComparing(newest));
        } else if ("views-all".equals(mode) || "views".equals(mode)) {
            tracks.sort(Comparator.comparingInt(Track::getViewsAll).reversed().thenComparing(newest));
        } else {
            tracks.sort(newest);
        }
        return tracks;
    }

    public void recordView(Integer trackId) {
        if (trackId == null || trackRepository.findById(trackId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu");
        }
        TrackView view = new TrackView();
        view.setTrackId(trackId);
        view.setPlayedAt(Instant.now());
        viewRepository.save(view);
    }

    public List<Track> decorateAll(List<Track> tracks) {
        if (tracks.isEmpty()) {
            return tracks;
        }
        List<Integer> ids = tracks.stream().map(Track::getId).toList();
        Map<Integer, int[]> votes = voteTotals(ids);
        Map<Integer, Integer> viewsMonth = viewCounts(ids, monthStart());
        Map<Integer, Integer> viewsYear = viewCounts(ids, yearStart());
        Map<Integer, Integer> viewsAll = viewCounts(ids, null);
        Map<Integer, Integer> comments = commentCounts(ids);
        Set<Integer> downloadable = downloadableIds();
        int slots = downloadSlots();
        Map<Integer, String> ranks = new HashMap<>();
        for (Track track : tracks) {
            if (track.getAuthor() != null) {
                track.setSharedBy(track.getAuthor().getUsername());
                track.setAuthorRank(ranks.computeIfAbsent(track.getAuthor().getId(), id -> profileService.badge(track.getAuthor())));
            }
            int[] pair = votes.getOrDefault(track.getId(), new int[2]);
            track.setLikeCount(pair[0]);
            track.setDislikeCount(pair[1]);
            track.setScore(pair[0] - pair[1]);
            track.setViewsMonth(viewsMonth.getOrDefault(track.getId(), 0));
            track.setViewsYear(viewsYear.getOrDefault(track.getId(), 0));
            track.setViewsAll(viewsAll.getOrDefault(track.getId(), 0));
            track.setCommentCount(comments.getOrDefault(track.getId(), 0));
            track.setHasCover(coverStorage.exists(track.getId()));
            String file = downloadLink(track.getUrl());
            track.setDownloadUrl(file != null && downloadable.contains(track.getId()) ? file : null);
            track.setDownloadSlots(slots);
            if (track.getCreatedAt() != null) {
                track.setEditableUntil(track.getCreatedAt().plus(EDIT_WINDOW).toString());
            }
            track.setEditable(canEdit(track));
        }
        return tracks;
    }

    public Track decorate(Track track) {
        decorateAll(List.of(track));
        return track;
    }

    public Track share(Track incoming) {
        if (incoming.getArtist() == null || incoming.getArtist().isBlank()
                || incoming.getTitle() == null || incoming.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist and title are required");
        }
        String url = canonical(incoming.getUrl());
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj link z hostuje.net albo soundfiles.eu");
        }
        Track track = Track.builder()
                .artist(incoming.getArtist().trim())
                .title(incoming.getTitle().trim())
                .version(incoming.getVersion() == null ? "" : incoming.getVersion().trim())
                .url(url)
                .genre(normalizeGenre(incoming.getGenre()))
                .points(0)
                .createdAt(Instant.now())
                .author(currentUser())
                .build();
        return decorate(trackRepository.save(track));
    }

    public Track update(Integer trackId, Track incoming) {
        Track track = requireTrack(trackId);
        requireEditable(track);
        if (incoming.getArtist() == null || incoming.getArtist().isBlank()
                || incoming.getTitle() == null || incoming.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist and title are required");
        }
        String url = canonical(incoming.getUrl());
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj link z hostuje.net albo soundfiles.eu");
        }
        track.setArtist(incoming.getArtist().trim());
        track.setTitle(incoming.getTitle().trim());
        track.setVersion(incoming.getVersion() == null ? "" : incoming.getVersion().trim());
        track.setUrl(url);
        track.setGenre(normalizeGenre(incoming.getGenre()));
        return decorate(trackRepository.save(track));
    }

    public void saveCover(Integer trackId, MultipartFile file) {
        Track track = requireTrack(trackId);
        requireEditable(track);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz obrazek");
        }
        if (file.getSize() > CoverImages.MAX_BYTES_IN) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Obrazek może mieć najwyżej 2 MB");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plik musi być obrazkiem");
        }
        try {
            coverStorage.write(trackId, CoverImages.miniaturize(file.getBytes()));
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie udało się odczytać obrazka");
        }
    }

    public byte[] cover(Integer trackId) {
        requireTrack(trackId);
        return coverStorage.read(trackId);
    }

    public List<CommentView> comments(Integer trackId) {
        requireTrack(trackId);
        return commentRepository.findByTrackIdOrderByCreatedAtAsc(trackId).stream()
                .map(comment -> new CommentView(
                        comment.getId(),
                        comment.getText(),
                        comment.getAuthor().getUsername(),
                        comment.getCreatedAt().toString()))
                .toList();
    }

    public CommentView addComment(Integer trackId, String text) {
        Track track = requireTrack(trackId);
        if (text == null || text.isBlank() || text.trim().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment text is required");
        }
        TrackComment comment = new TrackComment();
        comment.setText(text.trim());
        comment.setCreatedAt(Instant.now());
        comment.setTrack(track);
        comment.setAuthor(currentUser());
        TrackComment saved = commentRepository.save(comment);
        return new CommentView(saved.getId(), saved.getText(), saved.getAuthor().getUsername(), saved.getCreatedAt().toString());
    }

    public LikeState likes(Integer trackId) {
        requireTrack(trackId);
        int[] pair = voteTotals(List.of(trackId)).getOrDefault(trackId, new int[2]);
        int mine = currentUserOptional()
                .flatMap(user -> likeRepository.findByTrackIdAndUserId(trackId, user.getId()))
                .map(like -> like.getVote() == null || like.getVote() >= 0 ? 1 : -1)
                .orElse(0);
        return new LikeState(pair[0], pair[1], pair[0] - pair[1], mine);
    }

    public LikeState vote(Integer trackId, int value) {
        if (value != 1 && value != -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ocena to polubienie albo odrzucenie");
        }
        Track track = requireTrack(trackId);
        User user = currentUser();
        Optional<TrackLike> existing = likeRepository.findByTrackIdAndUserId(trackId, user.getId());
        if (existing.isPresent()) {
            int current = existing.get().getVote() == null ? 1 : existing.get().getVote();
            if (current == value) {
                likeRepository.delete(existing.get());
            } else {
                existing.get().setVote(value);
                likeRepository.save(existing.get());
            }
        } else {
            TrackLike like = new TrackLike();
            like.setTrack(track);
            like.setUser(user);
            like.setVote(value);
            likeRepository.save(like);
        }
        LikeState state = likes(trackId);
        track.setPoints(state.score());
        trackRepository.save(track);
        return state;
    }

    private Instant monthStart() {
        ZonedDateTime now = ZonedDateTime.now(WARSAW);
        return now.toLocalDate().withDayOfMonth(1).atStartOfDay(WARSAW).toInstant();
    }

    private Instant yearStart() {
        ZonedDateTime now = ZonedDateTime.now(WARSAW);
        return now.toLocalDate().withDayOfYear(1).atStartOfDay(WARSAW).toInstant();
    }

    private Map<Integer, Integer> commentCounts(List<Integer> ids) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : commentRepository.countsForTracks(ids)) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        return counts;
    }

    private Map<Integer, Integer> viewCounts(Collection<Integer> ids, Instant since) {
        Map<Integer, Integer> totals = new HashMap<>();
        List<Integer> present = ids.stream().filter(id -> id != null).toList();
        if (present.isEmpty()) {
            return totals;
        }
        List<Object[]> rows = since == null
                ? viewRepository.countsAll(present)
                : viewRepository.countsSince(present, since);
        for (Object[] row : rows) {
            totals.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        return totals;
    }

    private Map<Integer, int[]> voteTotals(Collection<Integer> ids) {
        Map<Integer, int[]> totals = new HashMap<>();
        List<Integer> present = ids.stream().filter(id -> id != null).toList();
        if (present.isEmpty()) {
            return totals;
        }
        for (Object[] row : likeRepository.votesFor(present)) {
            int id = ((Number) row[0]).intValue();
            int vote = ((Number) row[1]).intValue();
            int[] pair = totals.computeIfAbsent(id, key -> new int[2]);
            if (vote < 0) {
                pair[1]++;
            } else {
                pair[0]++;
            }
        }
        return totals;
    }

    private Set<Integer> downloadableIds() {
        Optional<User> user = currentUserOptional();
        if (user.isEmpty()) {
            return Set.of();
        }
        int slots = profileService.activityOf(user.get().getId()) * 10;
        if (slots <= 0) {
            return Set.of();
        }
        Set<Integer> ids = new HashSet<>();
        for (Object[] row : trackRepository.communityIdsAndUrls()) {
            if (ids.size() >= slots) {
                break;
            }
            String url = row[1] == null ? "" : row[1].toString();
            if (downloadLink(url) != null) {
                ids.add(((Number) row[0]).intValue());
            }
        }
        return ids;
    }

    private int downloadSlots() {
        return currentUserOptional()
                .map(user -> profileService.activityOf(user.getId()) * 10)
                .orElse(0);
    }

    static String hostujeFile(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = HOSTUJE_LINK.matcher(url.trim());
        if (!matcher.find()) {
            return null;
        }
        String hash = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return "https://hostuje.net/file.php?id=" + hash.toLowerCase();
    }

    static String soundfilesId(String url) {
        if (url == null) {
            return null;
        }
        String value = url.trim();
        Matcher matcher = SOUNDFILES_LINK.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return SOUNDFILES_ID.matcher(value).matches() ? value : null;
    }

    static String canonical(String url) {
        String hostuje = hostujeFile(url);
        if (hostuje != null) {
            return hostuje;
        }
        String id = soundfilesId(url);
        return id == null ? null : "https://soundfiles.eu/" + id + "/file";
    }

    static String downloadLink(String url) {
        String hostuje = hostujeFile(url);
        if (hostuje != null) {
            return hostuje;
        }
        String id = soundfilesId(url);
        return id == null ? null : "https://soundfiles.eu/" + id + "/file/download";
    }

    private Track requireTrack(Integer trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
    }

    private String normalizeGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genre is required");
        }
        try {
            String key = genre.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if ("BIGROOM".equals(key)) {
                key = "BIG_ROOM";
            }
            return Genre.valueOf(key).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown genre");
        }
    }

    public void delete(Integer trackId) {
        Track track = requireTrack(trackId);
        User user = currentUser();
        boolean author = track.getAuthor() != null && track.getAuthor().getId().equals(user.getId());
        if (!author && !isStaff()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utwór może usunąć tylko autor albo moderator");
        }
        likeRepository.deleteByTrackId(trackId);
        commentRepository.deleteByTrackId(trackId);
        viewRepository.deleteByTrackId(trackId);
        entryRepository.deleteByTrack_Id(trackId);
        fileRepository.deleteById(trackId);
        trackRepository.delete(track);
        coverStorage.delete(trackId);
    }

    private boolean isStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_MODERATOR".equals(authority.getAuthority()));
    }

    private User currentUser() {
        return currentUserOptional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required"));
    }

    private Optional<User> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    private boolean canEdit(Track track) {
        if (track.getCreatedAt() == null || track.getAuthor() == null) {
            return false;
        }
        User user = currentUserOptional().orElse(null);
        if (user == null || !track.getAuthor().getId().equals(user.getId())) {
            return false;
        }
        return track.getCreatedAt().plus(EDIT_WINDOW).isAfter(Instant.now());
    }

    private void requireEditable(Track track) {
        currentUser();
        if (!canEdit(track)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Utwór może poprawić tylko autor, i tylko przez 24 godziny od dodania");
        }
    }
}
