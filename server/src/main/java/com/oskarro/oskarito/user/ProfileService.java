package com.oskarro.oskarito.user;

import com.oskarro.oskarito.track.CoverStorage;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackCommentRepository;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import org.springframework.http.HttpStatus;
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

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final TrackLikeRepository likeRepository;
    private final TrackCommentRepository commentRepository;
    private final CoverStorage coverStorage;

    public ProfileService(UserRepository userRepository,
                          TrackRepository trackRepository,
                          TrackLikeRepository likeRepository,
                          TrackCommentRepository commentRepository,
                          CoverStorage coverStorage) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.coverStorage = coverStorage;
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
        Map<Integer, Integer> commentsOnTracks = counts(commentRepository.countGroupedByTrackAuthor());
        Map<Integer, Integer> likes = counts(likeRepository.likesGroupedByAuthor());
        Map<Integer, Integer> dislikes = counts(likeRepository.dislikesGroupedByAuthor());
        List<Standing> standings = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            int id = user.getId();
            int trackCount = tracks.getOrDefault(id, 0);
            int monthCount = month.getOrDefault(id, 0);
            int written = commentsWritten.getOrDefault(id, 0);
            int onTracks = commentsOnTracks.getOrDefault(id, 0);
            int score = score(trackCount, likes.getOrDefault(id, 0), dislikes.getOrDefault(id, 0), onTracks, written);
            if (trackCount == 0 && score == 0) {
                continue;
            }
            standings.add(new Standing(user.getUsername(), activityRank(score), trackCount, monthCount, score));
        }
        return new RankingView(
                "Utwór, który dodasz, daje 8 pkt. Komentarz, który napiszesz, daje 1 pkt. Komentarz pod Twoim utworem daje 2 pkt. Każda przewaga polubień nad minusami daje 3 pkt.",
                List.of(
                        new RankingView.RankStep("Słuchacz", 0, "Start. Wystarczy konto."),
                        new RankingView.RankStep("Bywalec", 15, "15 punktów. Wystarczą 2 dodane utwory albo 15 komentarzy."),
                        new RankingView.RankStep("Rezydent", 40, "40 punktów. Na przykład 5 utworów."),
                        new RankingView.RankStep("Selekcjoner", 90, "90 punktów. Na przykład 12 utworów albo mniej, gdy zbierają głosy i komentarze."),
                        new RankingView.RankStep("Gospodarz", 180, "180 punktów. Na przykład 23 utwory albo mieszanka utworów, głosów i komentarzy.")
                ),
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

    public ProfileView updateBio(String bio) {
        User user = currentUser();
        String text = bio == null ? "" : bio.trim();
        if (text.length() > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opis może mieć najwyżej 280 znaków");
        }
        user.setBio(text.isEmpty() ? null : text);
        userRepository.save(user);
        return viewOf(user, true);
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
        int score = score(tracks, likes, dislikes, commentsOnTracks, commentsWritten);
        String activity = activityRank(score);
        String override = user.getRankOverride();
        boolean granted = override != null && !override.isBlank();
        int activityCount = commentsWritten + (int) likeRepository.countByUserId(user.getId());
        List<Track> owned = trackRepository.findByAuthor_IdOrderByIdDesc(user.getId());
        Map<Integer, int[]> votes = voteTotals(owned.stream().map(Track::getId).toList());
        List<ProfileTrack> shared = owned.stream()
                .limit(30)
                .map(track -> {
                    int[] pair = votes.getOrDefault(track.getId(), new int[2]);
                    return new ProfileTrack(
                            track.getId(),
                            track.getTitle(),
                            track.getArtist(),
                            track.getVersion(),
                            track.getGenre(),
                            pair[0] - pair[1],
                            coverStorage.exists(track.getId()));
                })
                .toList();
        return new ProfileView(
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getBio(),
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
                activityCount * 10,
                isAdmin(),
                shared);
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
        int dislikes = (int) likeRepository.countDislikesReceived(userId);
        int commentsOnTracks = (int) commentRepository.countByTrack_Author_Id(userId);
        int commentsWritten = (int) commentRepository.countByAuthorId(userId);
        return score(tracks, likes, dislikes, commentsOnTracks, commentsWritten);
    }

    static int score(int tracks, int likes, int dislikes, int commentsOnTracks, int commentsWritten) {
        int popularity = Math.max(0, likes - dislikes);
        return tracks * 8 + popularity * 3 + commentsOnTracks * 2 + commentsWritten;
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
                .orElse(new Standing(username, "Słuchacz", 0, 0, 0));
        int[] steps = {15, 40, 90, 180};
        String[] names = {"Bywalec", "Rezydent", "Selekcjoner", "Gospodarz"};
        for (int i = 0; i < steps.length; i++) {
            if (mine.score < steps[i]) {
                return new RankingView.Progress(username, mine.score, mine.rank, names[i], steps[i] - mine.score);
            }
        }
        return new RankingView.Progress(username, mine.score, mine.rank, null, null);
    }

    private record Standing(String username, String rank, int tracks, int monthTracks, int score) {
    }

    static String activityRank(int score) {
        if (score >= 180) {
            return "Gospodarz";
        }
        if (score >= 90) {
            return "Selekcjoner";
        }
        if (score >= 40) {
            return "Rezydent";
        }
        if (score >= 15) {
            return "Bywalec";
        }
        return "Słuchacz";
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
