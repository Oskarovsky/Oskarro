package com.oskarro.oskarito.admin;

import com.oskarro.oskarito.admin.AdminViews.*;
import com.oskarro.oskarito.report.ReportService;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackComment;
import com.oskarro.oskarito.track.TrackCommentRepository;
import com.oskarro.oskarito.track.TrackLike;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackSocialService;
import com.oskarro.oskarito.track.TrackViewRepository;
import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Everything the admin panel reads and changes. The site is small, so lists are filtered
 * and paged in memory; every change is written to the audit trail ({@link AdminAction}).
 */
@Service
@Transactional
public class AdminService {

    private static final Pattern RANK_TITLE = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 '\\-]{1,31}$");
    private static final int MAX_PAGE = 100;

    private final UserRepository users;
    private final RoleRepository roles;
    private final TrackRepository tracks;
    private final TrackCommentRepository comments;
    private final TrackLikeRepository votes;
    private final TrackViewRepository plays;
    private final TrackSocialService social;
    private final AdminActionRepository audit;
    private final ReportService reports;

    public AdminService(UserRepository users, RoleRepository roles, TrackRepository tracks,
                        TrackCommentRepository comments, TrackLikeRepository votes, TrackViewRepository plays,
                        TrackSocialService social, AdminActionRepository audit, ReportService reports) {
        this.users = users;
        this.roles = roles;
        this.tracks = tracks;
        this.comments = comments;
        this.votes = votes;
        this.plays = plays;
        this.social = social;
        this.audit = audit;
        this.reports = reports;
    }

    // ---------- dashboard ----------

    @Transactional(readOnly = true)
    public Overview overview() {
        Instant now = Instant.now();
        List<User> all = users.findAll();
        Map<Integer, Long> trackCounts = counts(tracks.countGroupedByAuthor());
        Map<Integer, Long> commentCounts = counts(comments.countGroupedByAuthor());
        Map<Integer, Long> voteCounts = counts(votes.countGroupedByVoter());
        List<UserRow> latest = all.stream()
                .sorted(newestFirst())
                .limit(6)
                .map(user -> row(user, trackCounts, commentCounts, voteCounts))
                .toList();
        return new Overview(
                all.size(),
                all.stream().filter(since(User::getCreatedAt, now.minus(Duration.ofDays(7)))).count(),
                all.stream().filter(since(User::getCreatedAt, now.minus(Duration.ofDays(30)))).count(),
                all.stream().filter(since(User::getLastSeenAt, now.minus(Duration.ofHours(24)))).count(),
                all.stream().filter(since(User::getLastSeenAt, now.minus(Duration.ofDays(7)))).count(),
                all.stream().filter(since(User::getLastSeenAt, now.minus(Duration.ofDays(30)))).count(),
                all.stream().filter(user -> Boolean.FALSE.equals(user.getActivated())).count(),
                all.stream().filter(user -> Boolean.TRUE.equals(user.getBanned())).count(),
                all.stream().filter(this::isStaff).count(),
                tracks.countCommunity(),
                tracks.findCommunityCreatedSince(now.minus(Duration.ofDays(7))).size(),
                comments.count(),
                comments.createdSince(now.minus(Duration.ofDays(7))).size(),
                votes.count(),
                plays.count(),
                plays.playedSince(now.minus(Duration.ofDays(7))).size(),
                reports.openCount(),
                latest,
                events("all").stream().limit(12).toList());
    }

    // ---------- users ----------

    @Transactional(readOnly = true)
    public Page<UserRow> users(String query, String filter, String sort, int page, int size) {
        Instant now = Instant.now();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Predicate<User> matches = user -> q.isEmpty()
                || user.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(q));
        Predicate<User> inFilter = switch (filter == null ? "all" : filter) {
            case "active" -> since(User::getLastSeenAt, now.minus(Duration.ofDays(7)));
            case "inactive" -> user -> user.getLastSeenAt() == null || user.getLastSeenAt().isBefore(now.minus(Duration.ofDays(30)));
            case "unactivated" -> user -> Boolean.FALSE.equals(user.getActivated());
            case "banned" -> user -> Boolean.TRUE.equals(user.getBanned());
            case "staff" -> this::isStaff;
            default -> user -> true;
        };
        Map<Integer, Long> trackCounts = counts(tracks.countGroupedByAuthor());
        Map<Integer, Long> commentCounts = counts(comments.countGroupedByAuthor());
        Map<Integer, Long> voteCounts = counts(votes.countGroupedByVoter());
        Comparator<User> order = switch (sort == null ? "new" : sort) {
            case "seen" -> Comparator.comparing(User::getLastSeenAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case "tracks" -> Comparator.comparing((User user) -> trackCounts.getOrDefault(user.getId(), 0L)).reversed();
            case "name" -> Comparator.comparing(user -> user.getUsername().toLowerCase(Locale.ROOT));
            default -> newestFirst();
        };
        List<UserRow> rows = users.findAll().stream()
                .filter(matches.and(inFilter))
                .sorted(order)
                .map(user -> row(user, trackCounts, commentCounts, voteCounts))
                .toList();
        return page(rows, page, size);
    }

    @Transactional(readOnly = true)
    public UserDetail user(Integer id) {
        User user = requireUser(id);
        Map<Integer, Long> trackCounts = counts(tracks.countGroupedByAuthor());
        Map<Integer, Long> commentCounts = counts(comments.countGroupedByAuthor());
        Map<Integer, Long> voteCounts = counts(votes.countGroupedByVoter());
        List<TrackRow> owned = social.decorateAll(new ArrayList<>(tracks.findTop20ByAuthor_IdOrderByIdDesc(id)))
                .stream().map(this::trackRow).toList();
        List<CommentRow> written = comments.findTop20ByAuthor_IdOrderByCreatedAtDesc(id).stream().map(this::commentRow).toList();
        List<VoteRow> cast = votes.findTop20ByUser_IdOrderByIdDesc(id).stream()
                .map(vote -> new VoteRow(vote.getTrack().getId(), vote.getTrack().getTitle(),
                        vote.getVote() == null || vote.getVote() >= 0 ? 1 : -1, iso(vote.getCreatedAt())))
                .toList();
        List<AuditRow> history = audit.findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc("user", id).stream()
                .map(this::auditRow).toList();
        return new UserDetail(row(user, trackCounts, commentCounts, voteCounts), user.getBio(), user.getWebsite(),
                owned, written, cast, history);
    }

    public UserDetail changeUser(Integer id, UserChange change) {
        User user = requireUser(id);
        String actor = actor();
        boolean self = user.getUsername().equals(actor);
        List<String> notes = new ArrayList<>();

        if (change.activated() != null && !change.activated().equals(user.getActivated())) {
            user.setActivated(change.activated());
            notes.add(change.activated() ? "aktywował konto" : "dezaktywował konto");
        }
        if (change.banned() != null && change.banned() != Boolean.TRUE.equals(user.getBanned())) {
            if (self) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie możesz zablokować własnego konta");
            }
            user.setBanned(change.banned());
            String reason = change.banReason() == null ? "" : change.banReason().trim();
            if (reason.length() > 200) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Powód blokady: najwyżej 200 znaków");
            }
            user.setBanReason(change.banned() && !reason.isEmpty() ? reason : null);
            notes.add(change.banned() ? "zablokował konto" + (reason.isEmpty() ? "" : " (" + reason + ")") : "odblokował konto");
        }
        if (change.roles() != null) {
            Set<RoleName> wanted = change.roles().stream().map(this::roleName).collect(Collectors.toCollection(TreeSet::new));
            wanted.add(RoleName.ROLE_USER);
            Set<RoleName> current = user.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(TreeSet::new));
            if (!wanted.equals(current)) {
                if (self && current.contains(RoleName.ROLE_ADMIN) && !wanted.contains(RoleName.ROLE_ADMIN)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie możesz odebrać sobie roli administratora");
                }
                Set<Role> next = new HashSet<>();
                for (RoleName name : wanted) {
                    next.add(roles.findByName(name).orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Brak roli " + name)));
                }
                user.setRoles(next);
                notes.add("ustawił role: " + wanted.stream().map(this::roleLabel).collect(Collectors.joining(", ")));
            }
        }
        if (change.rankOverride() != null) {
            String title = change.rankOverride().trim();
            if (!title.isEmpty() && !RANK_TITLE.matcher(title).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ranga: 2–32 znaki, litery, cyfry, spacja albo myślnik");
            }
            String next = title.isEmpty() ? null : title;
            if (!Objects.equals(next, user.getRankOverride())) {
                user.setRankOverride(next);
                notes.add(next == null ? "usunął rangę specjalną" : "nadał rangę „" + next + "”");
            }
        }
        if (!notes.isEmpty()) {
            users.save(user);
            log("user.update", "user", user.getId(), user.getUsername(), String.join("; ", notes));
        }
        return user(id);
    }

    // ---------- content ----------

    @Transactional(readOnly = true)
    public Page<TrackRow> tracks(String query, String genre, int page, int size) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String g = genre == null ? "" : genre.trim().toUpperCase(Locale.ROOT);
        List<Track> found = tracks.findAll().stream()
                .filter(track -> track.getGenre() != null && !track.getGenre().isBlank())
                .filter(track -> g.isEmpty() || g.equals(track.getGenre()))
                .filter(track -> q.isEmpty() || contains(track.getTitle(), q) || contains(track.getArtist(), q)
                        || (track.getAuthor() != null && contains(track.getAuthor().getUsername(), q)))
                .sorted(Comparator.comparing(Track::getId, Comparator.reverseOrder()))
                .toList();
        int from = Math.min(Math.max(page, 0) * clamp(size), found.size());
        int to = Math.min(from + clamp(size), found.size());
        List<TrackRow> rows = social.decorateAll(new ArrayList<>(found.subList(from, to))).stream().map(this::trackRow).toList();
        return new Page<>(rows, Math.max(page, 0), clamp(size), found.size());
    }

    public TrackRow changeTrack(Integer id, TrackChange change) {
        Track track = tracks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        if (blank(change.artist()) || blank(change.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wykonawca i tytuł są wymagane");
        }
        String url = TrackSocialService.canonical(change.url());
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj link z hostuje.net albo soundfiles.eu");
        }
        String genre;
        try {
            genre = Genre.valueOf(change.genre().trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_')).name();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany gatunek");
        }
        List<String> notes = new ArrayList<>();
        noteChange(notes, "wykonawca", track.getArtist(), change.artist().trim());
        noteChange(notes, "tytuł", track.getTitle(), change.title().trim());
        noteChange(notes, "wersja", track.getVersion(), change.version() == null ? "" : change.version().trim());
        noteChange(notes, "gatunek", track.getGenre(), genre);
        noteChange(notes, "link", track.getUrl(), url);
        track.setArtist(change.artist().trim());
        track.setTitle(change.title().trim());
        track.setVersion(change.version() == null ? "" : change.version().trim());
        track.setGenre(genre);
        track.setUrl(url);
        tracks.save(track);
        if (!notes.isEmpty()) {
            log("track.update", "track", id, track.getArtist() + " – " + track.getTitle(), String.join("; ", notes));
        }
        return trackRow(social.decorate(track));
    }

    public void deleteTrack(Integer id) {
        Track track = tracks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        String label = track.getArtist() + " – " + track.getTitle();
        String author = track.getAuthor() == null ? "—" : track.getAuthor().getUsername();
        social.delete(id);
        log("track.delete", "track", id, label, "autor: " + author);
    }

    @Transactional(readOnly = true)
    public Page<CommentRow> comments(String query, int page, int size) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CommentRow> rows = comments.findAll().stream()
                .filter(comment -> q.isEmpty() || contains(comment.getText(), q)
                        || contains(comment.getAuthor().getUsername(), q) || contains(comment.getTrack().getTitle(), q))
                .sorted(Comparator.comparing(TrackComment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::commentRow)
                .toList();
        return page(rows, page, size);
    }

    public CommentRow changeComment(Integer id, CommentChange change) {
        TrackComment comment = comments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego komentarza"));
        String text = change.text() == null ? "" : change.text().trim();
        if (text.isEmpty() || text.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Komentarz: od 1 do 1000 znaków");
        }
        String before = comment.getText();
        comment.setText(text);
        comments.save(comment);
        log("comment.update", "comment", id, "komentarz " + comment.getAuthor().getUsername(),
                "było: „" + shorten(before, 200) + "”");
        return commentRow(comment);
    }

    public void deleteComment(Integer id) {
        TrackComment comment = comments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego komentarza"));
        comments.delete(comment);
        log("comment.delete", "comment", id, "komentarz " + comment.getAuthor().getUsername(),
                "„" + shorten(comment.getText(), 200) + "” pod " + comment.getTrack().getTitle());
    }

    // ---------- activity & audit ----------

    @Transactional(readOnly = true)
    public Page<Event> activity(String type, int page, int size) {
        return page(events(type == null ? "all" : type), page, size);
    }

    @Transactional(readOnly = true)
    public Page<AuditRow> auditLog(int page, int size) {
        return page(audit.findTop200ByOrderByCreatedAtDesc().stream().map(this::auditRow).toList(), page, size);
    }

    private List<Event> events(String type) {
        Stream<Event> stream = Stream.empty();
        if (type.equals("all") || type.equals("users")) {
            stream = Stream.concat(stream, users.findAll().stream()
                    .filter(user -> user.getCreatedAt() != null)
                    .map(user -> new Event("user", iso(user.getCreatedAt()), user.getUsername(),
                            "założył konto", "user", user.getId())));
        }
        if (type.equals("all") || type.equals("tracks")) {
            stream = Stream.concat(stream, tracks.findTop200ByCreatedAtNotNullOrderByCreatedAtDesc().stream()
                    .filter(track -> track.getAuthor() != null)
                    .map(track -> new Event("track", iso(track.getCreatedAt()), track.getAuthor().getUsername(),
                            "dodał utwór „" + track.getArtist() + " – " + track.getTitle() + "”", "track", track.getId())));
        }
        if (type.equals("all") || type.equals("comments")) {
            stream = Stream.concat(stream, comments.findTop200ByOrderByCreatedAtDesc().stream()
                    .map(comment -> new Event("comment", iso(comment.getCreatedAt()), comment.getAuthor().getUsername(),
                            "skomentował „" + comment.getTrack().getTitle() + "”: " + shorten(comment.getText(), 90),
                            "track", comment.getTrack().getId())));
        }
        if (type.equals("all") || type.equals("votes")) {
            stream = Stream.concat(stream, votes.findTop200ByCreatedAtNotNullOrderByCreatedAtDesc().stream()
                    .map(vote -> new Event("vote", iso(vote.getCreatedAt()), vote.getUser().getUsername(),
                            "ocenił „" + vote.getTrack().getTitle() + "” na " + (vote.getVote() != null && vote.getVote() < 0 ? "−1" : "+1"),
                            "track", vote.getTrack().getId())));
        }
        if (type.equals("all") || type.equals("admin")) {
            stream = Stream.concat(stream, audit.findTop200ByOrderByCreatedAtDesc().stream()
                    .map(action -> new Event("admin", iso(action.getCreatedAt()), action.getActor(),
                            describe(action), action.getTargetType(), action.getTargetId())));
        }
        return stream.sorted(Comparator.comparing(Event::at, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    // ---------- helpers ----------

    private void log(String action, String targetType, Integer targetId, String label, String details) {
        AdminAction entry = new AdminAction();
        entry.setActor(actor());
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTargetLabel(shorten(label, 120));
        entry.setDetails(shorten(details, 500));
        entry.setCreatedAt(Instant.now());
        audit.save(entry);
    }

    private String describe(AdminAction action) {
        String what = switch (action.getAction()) {
            case "user.update" -> "zmienił konto " + action.getTargetLabel();
            case "track.update" -> "poprawił utwór „" + action.getTargetLabel() + "”";
            case "track.delete" -> "usunął utwór „" + action.getTargetLabel() + "”";
            case "comment.update" -> "poprawił " + action.getTargetLabel();
            case "comment.delete" -> "usunął " + action.getTargetLabel();
            default -> action.getAction();
        };
        return what + (action.getDetails() == null ? "" : ": " + action.getDetails());
    }

    private UserRow row(User user, Map<Integer, Long> trackCounts, Map<Integer, Long> commentCounts, Map<Integer, Long> voteCounts) {
        List<String> roleNames = user.getRoles().stream().map(role -> role.getName().name()).sorted().toList();
        return new UserRow(user.getId(), user.getUsername(), user.getEmail(), iso(user.getCreatedAt()),
                iso(user.getLastLoginAt()), iso(user.getLastSeenAt()),
                user.getActivated() == null || user.getActivated(), Boolean.TRUE.equals(user.getBanned()),
                user.getBanReason(), roleNames, user.getRankOverride(),
                trackCounts.getOrDefault(user.getId(), 0L), commentCounts.getOrDefault(user.getId(), 0L),
                voteCounts.getOrDefault(user.getId(), 0L));
    }

    private TrackRow trackRow(Track track) {
        return new TrackRow(track.getId(), track.getTitle(), track.getArtist(), track.getVersion(), track.getGenre(),
                track.getUrl(), track.getAuthor() == null ? null : track.getAuthor().getUsername(), iso(track.getCreatedAt()),
                track.getScore() == null ? 0 : track.getScore(), track.getViewsAll() == null ? 0 : track.getViewsAll(),
                track.getCommentCount() == null ? 0 : track.getCommentCount());
    }

    private CommentRow commentRow(TrackComment comment) {
        return new CommentRow(comment.getId(), comment.getText(), comment.getAuthor().getUsername(),
                comment.getTrack().getId(), comment.getTrack().getTitle(), iso(comment.getCreatedAt()));
    }

    private AuditRow auditRow(AdminAction action) {
        return new AuditRow(action.getId(), action.getActor(), action.getAction(), action.getTargetType(),
                action.getTargetId(), action.getTargetLabel(), action.getDetails(), iso(action.getCreatedAt()));
    }

    private boolean isStaff(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN || role.getName() == RoleName.ROLE_MODERATOR);
    }

    private RoleName roleName(String raw) {
        String key = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "ADMIN", "ROLE_ADMIN" -> RoleName.ROLE_ADMIN;
            case "MODERATOR", "MOD", "ROLE_MODERATOR" -> RoleName.ROLE_MODERATOR;
            case "USER", "ROLE_USER" -> RoleName.ROLE_USER;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznana rola: " + raw);
        };
    }

    private String roleLabel(RoleName name) {
        return switch (name) {
            case ROLE_ADMIN -> "administrator";
            case ROLE_MODERATOR -> "moderator";
            default -> "użytkownik";
        };
    }

    private User requireUser(Integer id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego użytkownika"));
    }

    private static String actor() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static Comparator<User> newestFirst() {
        return Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(User::getId, Comparator.reverseOrder());
    }

    private static Predicate<User> since(java.util.function.Function<User, Instant> field, Instant from) {
        return user -> field.apply(user) != null && !field.apply(user).isBefore(from);
    }

    private static Map<Integer, Long> counts(List<Object[]> rows) {
        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                result.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }
        return result;
    }

    private static <T> Page<T> page(List<T> rows, int page, int size) {
        int perPage = clamp(size);
        int current = Math.max(page, 0);
        int from = Math.min(current * perPage, rows.size());
        int to = Math.min(from + perPage, rows.size());
        return new Page<>(rows.subList(from, to), current, perPage, rows.size());
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE));
    }

    private static void noteChange(List<String> notes, String field, String before, String after) {
        if (!Objects.equals(before == null ? "" : before, after == null ? "" : after)) {
            notes.add(field + ": „" + shorten(before, 80) + "” → „" + shorten(after, 80) + "”");
        }
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
