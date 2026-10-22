package com.oskarro.oskarito.user;

import com.oskarro.oskarito.track.CoverStorage;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackCommentRepository;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackViewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProfileService {

    private static final Pattern RANK_TITLE = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 '\\-]{1,31}$");
    private static final String[] RANK_NAMES = {
            "Słuchacz", "Bywalec", "Rezydent", "Selekcjoner", "Didżej", "Gwiazda", "Legenda"
    };
    private static final int[] RANK_SCORES = {0, 500, 1500, 3500, 7000, 12000, 20000};
    private static final String[] RANK_HOW = {
            "Start. Wystarczy konto.",
            "500 punktów. Na przykład 100 dodanych utworów albo 250 komentarzy.",
            "1500 punktów. Na przykład 300 utworów albo 200 utworów i 250 komentarzy.",
            "3500 punktów. Na przykład 700 utworów, albo mniej, gdy dochodzą komentarze i polubienia.",
            "7000 punktów. Na przykład 1000 utworów i długa seria komentarzy.",
            "12000 punktów. Parkiet już Cię zna.",
            "20000 punktów. Najwyższa ranga, zbierana utworami, komentarzami i polubieniami."
    };

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final TrackLikeRepository likeRepository;
    private final TrackCommentRepository commentRepository;
    private final TrackViewRepository viewRepository;
    private final CoverStorage coverStorage;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository,
                          TrackRepository trackRepository,
                          TrackLikeRepository likeRepository,
                          TrackCommentRepository commentRepository,
                          TrackViewRepository viewRepository,
                          CoverStorage coverStorage,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.viewRepository = viewRepository;
        this.coverStorage = coverStorage;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileView me() {
        return viewOf(currentUser(), true);
    }

    @Transactional(readOnly = true)
    public RankingView ranking() {
        Map<Integer, Integer> tracks = counts(trackRepository.countGroupedByAuthor());
        Map<Integer, Integer> month = counts(trackRepository.countGroupedByAuthorSince(monthStart()));
        Map<Integer, Integer> commentsWritten = counts(commentRepository.countGroupedByAuthor());
        Map<Integer, Integer> likes = counts(likeRepository.likesGroupedByAuthor());
        List<Standing> standings = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            int id = user.getId();
            int trackCount = tracks.getOrDefault(id, 0);
            int monthCount = month.getOrDefault(id, 0);
            int written = commentsWritten.getOrDefault(id, 0);
            int score = score(trackCount, likes.getOrDefault(id, 0), written);
            if (trackCount == 0 && score == 0) {
                continue;
            }
            standings.add(new Standing(user.getUsername(), activityRank(score), trackCount, monthCount, score));
        }
        return new RankingView(
                "Dodany utwór daje 5 pkt. Napisany komentarz daje 2 pkt. Polubienie zebrane przy Twoim utworze daje 1 pkt.",
                rankSteps(),
                board(standings, Comparator.comparingInt(Standing::tracks).reversed().thenComparing(Standing::score, Comparator.reverseOrder()).thenComparing(Standing::username), true, false),
                board(standings, Comparator.comparingInt(Standing::score).reversed().thenComparing(Standing::tracks, Comparator.reverseOrder()).thenComparing(Standing::username), false, false),
                board(standings, Comparator.comparingInt(Standing::monthTracks).reversed().thenComparing(Standing::score, Comparator.reverseOrder()).thenComparing(Standing::username), false, true),
                progress(standings));
    }

    @Transactional(readOnly = true)
    public ProfileView view(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego użytkownika"));
        User viewer = currentUserOptional().orElse(null);
        boolean self = viewer != null && viewer.getId().equals(user.getId());
        return viewOf(user, self);
    }

    public ProfileView update(Map<String, Object> body) {
        User user = currentUser();
        if (body.containsKey("bio")) {
            Object raw = body.get("bio");
            String text = raw == null ? "" : String.valueOf(raw).trim();
            if (text.length() > 280) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opis może mieć najwyżej 280 znaków");
            }
            user.setBio(text.isEmpty() ? null : text);
        }
        if (body.containsKey("autoplayNext")) {
            user.setAutoplayNext(asBoolean(body.get("autoplayNext")));
        }
        if (body.containsKey("firstName")) {
            user.setFirstName(limited(body.get("firstName"), 40));
        }
        if (body.containsKey("lastName")) {
            user.setLastName(limited(body.get("lastName"), 40));
        }
        if (body.containsKey("website")) {
            user.setWebsite(website(body.get("website")));
        }
        if (body.containsKey("favoriteGenres")) {
            user.setFavoriteGenres(genres(body.get("favoriteGenres")));
        }
        userRepository.save(user);
        return viewOf(user, true);
    }

    public void changePassword(String current, String next) {
        User user = currentUser();
        String now = current == null ? "" : current;
        String fresh = next == null ? "" : next;
        if (fresh.length() < 6 || fresh.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło musi mieć od 6 do 40 znaków");
        }
        if (!passwordEncoder.matches(now, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obecne hasło się nie zgadza");
        }
        user.setPassword(passwordEncoder.encode(fresh));
        userRepository.save(user);
    }

    private static String limited(Object raw, int max) {
        String text = raw == null ? "" : String.valueOf(raw).trim();
        if (text.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Za długie pole");
        }
        return text.isEmpty() ? null : text;
    }

    private static String website(Object raw) {
        String text = raw == null ? "" : String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > 120 || !(text.startsWith("https://") || text.startsWith("http://"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Strona musi zaczynać się od https://");
        }
        return text;
    }

    private static final java.util.Set<String> PUBLIC_GENRES = java.util.Set.of(
            "CLUB", "DANCE", "RETRO", "HOUSE", "VIXA", "BASS_HOUSE", "TECHNO", "BIG_ROOM", "TRANCE", "ELECTRO", "DISCO_POLO", "HANDS_UP", "POP");

    private static String genres(Object raw) {
        String text = raw == null ? "" : String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        java.util.LinkedHashSet<String> picked = new java.util.LinkedHashSet<>();
        for (String part : text.split(",")) {
            String id = part.trim().toUpperCase(java.util.Locale.ROOT);
            if (!id.isEmpty()) {
                if (!PUBLIC_GENRES.contains(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany gatunek");
                }
                picked.add(id);
            }
        }
        String joined = String.join(",", picked);
        if (joined.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Za dużo gatunków");
        }
        return joined.isEmpty() ? null : joined;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            return "true".equalsIgnoreCase(normalized) || "1".equals(normalized);
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    public ProfileView grantRank(String username, String title) {
        if (!isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rangę nadaje admin");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego użytkownika"));
        String text = title == null ? "" : title.trim();
        if (text.isEmpty()) {
            user.setRankOverride(null);
        } else if (!RANK_TITLE.matcher(text).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ranga: 2–32 znaki, litery, cyfry, spacja albo myślnik");
        } else {
            user.setRankOverride(text);
        }
        userRepository.save(user);
        User viewer = currentUser();
        return viewOf(user, viewer.getId().equals(user.getId()));
    }

    /**
     * Browsing and playing other people's playlists is a perk from the second rank (Bywalec, 500 points).
     * Staff and users with a special rank granted by an admin get it too. Private playlists stay owner-only.
     */
    public boolean hasPlaylistAccess(User user) {
        if (user == null) {
            return false;
        }
        boolean staff = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN || role.getName() == RoleName.ROLE_MODERATOR);
        boolean special = user.getRankOverride() != null && !user.getRankOverride().isBlank();
        return staff || special || scoreOf(user.getId()) >= RANK_SCORES[1];
    }

    public String playlistAccessRank() {
        return RANK_NAMES[1];
    }

    public String badge(User user) {
        if (user == null) {
            return null;
        }
        if (user.getRankOverride() != null && !user.getRankOverride().isBlank()) {
            return user.getRankOverride().trim();
        }
        return activityRank(scoreOf(user.getId()));
    }

    public int activityOf(Integer userId) {
        return (int) (commentRepository.countByAuthorId(userId) + likeRepository.countByUserId(userId));
    }

    private ProfileView viewOf(User user, boolean includeEmail) {
        int tracks = (int) trackRepository.countByAuthor_Id(user.getId());
        int likes = (int) likeRepository.countLikesReceived(user.getId());
        int dislikes = (int) likeRepository.countDislikesReceived(user.getId());
        int commentsOnTracks = (int) commentRepository.countByTrack_Author_Id(user.getId());
        int commentsWritten = (int) commentRepository.countByAuthorId(user.getId());
        int score = score(tracks, likes, commentsWritten);
        String activity = activityRank(score);
        String override = user.getRankOverride();
        boolean granted = override != null && !override.isBlank();
        int activityCount = commentsWritten + (int) likeRepository.countByUserId(user.getId());
        List<Track> owned = trackRepository.findByAuthor_IdOrderByIdDesc(user.getId());
        List<Track> shown = owned.stream().limit(30).toList();
        List<Integer> shownIds = shown.stream().map(Track::getId).toList();
        Map<Integer, int[]> votes = voteTotals(shownIds);
        Map<Integer, Integer> views = new HashMap<>();
        Map<Integer, Integer> comments = new HashMap<>();
        if (!shownIds.isEmpty()) {
            for (Object[] row : viewRepository.countsAll(shownIds)) {
                views.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }
            for (Object[] row : commentRepository.countsForTracks(shownIds)) {
                comments.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }
        }
        List<ProfileTrack> shared = shown.stream()
                .map(track -> {
                    int[] pair = votes.getOrDefault(track.getId(), new int[2]);
                    return new ProfileTrack(
                            track.getId(),
                            track.getTitle(),
                            track.getArtist(),
                            track.getVersion(),
                            track.getGenre(),
                            pair[0] - pair[1],
                            coverStorage.exists(track.getId()),
                            views.getOrDefault(track.getId(), 0),
                            comments.getOrDefault(track.getId(), 0),
                            track.getCreatedAt() == null ? null : track.getCreatedAt().toString());
                })
                .toList();
        return new ProfileView(
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getBio(),
                user.getFirstName(),
                user.getLastName(),
                user.getWebsite(),
                user.getFavoriteGenres(),
                granted ? override.trim() : activity,
                activity,
                granted,
                score,
                tracks,
                likes,
                dislikes,
                commentsOnTracks,
                commentsWritten,
                activityCount,
                user.getDownloadCredits() == null ? 0 : user.getDownloadCredits(),
                isAdmin(),
                !Boolean.FALSE.equals(user.getAutoplayNext()),
                shared,
                includeEmail ? (user.getAuthProvider() == null ? "password" : user.getAuthProvider()) : null);
    }

    private Map<Integer, int[]> voteTotals(List<Integer> ids) {
        Map<Integer, int[]> totals = new HashMap<>();
        if (ids.isEmpty()) {
            return totals;
        }
        for (Object[] row : likeRepository.votesFor(ids)) {
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

    private int scoreOf(Integer userId) {
        int tracks = (int) trackRepository.countByAuthor_Id(userId);
        int likes = (int) likeRepository.countLikesReceived(userId);
        int commentsWritten = (int) commentRepository.countByAuthorId(userId);
        return score(tracks, likes, commentsWritten);
    }

    static int score(int tracks, int likes, int commentsWritten) {
        return tracks * 5 + commentsWritten * 2 + Math.max(0, likes);
    }

    private static Map<Integer, Integer> counts(List<Object[]> rows) {
        Map<Integer, Integer> totals = new HashMap<>();
        for (Object[] row : rows) {
            totals.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        return totals;
    }

    private static Instant monthStart() {
        return ZonedDateTime.now(ZoneId.of("Europe/Warsaw"))
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    private static List<RankingView.BoardRow> board(List<Standing> standings, Comparator<Standing> order, boolean requireTracks, boolean requireMonth) {
        List<Standing> sorted = standings.stream()
                .filter(row -> !requireTracks || row.tracks > 0)
                .filter(row -> !requireMonth || row.monthTracks > 0)
                .sorted(order)
                .limit(15)
                .toList();
        List<RankingView.BoardRow> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Standing row = sorted.get(i);
            rows.add(new RankingView.BoardRow(i + 1, row.username, row.rank, row.tracks, row.monthTracks, row.score));
        }
        return rows;
    }

    private RankingView.Progress progress(List<Standing> standings) {
        Optional<User> viewer = currentUserOptional();
        if (viewer.isEmpty()) {
            return null;
        }
        String username = viewer.get().getUsername();
        Standing mine = standings.stream().filter(row -> row.username.equals(username)).findFirst()
                .orElse(new Standing(username, RANK_NAMES[0], 0, 0, 0));
        for (int i = 1; i < RANK_SCORES.length; i++) {
            if (mine.score < RANK_SCORES[i]) {
                return new RankingView.Progress(username, mine.score, mine.rank, RANK_NAMES[i], RANK_SCORES[i] - mine.score);
            }
        }
        return new RankingView.Progress(username, mine.score, mine.rank, null, null);
    }

    private static List<RankingView.RankStep> rankSteps() {
        List<RankingView.RankStep> steps = new ArrayList<>();
        for (int i = 0; i < RANK_NAMES.length; i++) {
            steps.add(new RankingView.RankStep(RANK_NAMES[i], RANK_SCORES[i], RANK_HOW[i]));
        }
        return steps;
    }

    private record Standing(String username, String rank, int tracks, int monthTracks, int score) {
    }

    static String activityRank(int score) {
        String rank = RANK_NAMES[0];
        for (int i = 0; i < RANK_NAMES.length; i++) {
            if (score >= RANK_SCORES[i]) {
                rank = RANK_NAMES[i];
            }
        }
        return rank;
    }

    private User currentUser() {
        return currentUserOptional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się"));
    }

    private Optional<User> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
