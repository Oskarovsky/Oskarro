package com.oskarro.oskarito.notify;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.user.MailService;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mails that bring people back: a note to the author when someone comments on or likes their track, and an
 * opt-in weekly list of new tracks in their favourite genres. Every mail carries a one-click unsubscribe link.
 */
@Service
public class NotificationService {

    static final String SITE = "https://oskarro.com";
    private static final Duration LIKE_PAUSE = Duration.ofHours(12);

    public enum Kind { ACTIVITY, DIGEST }

    private final MailService mail;
    private final UserRepository users;
    private final TrackRepository tracks;
    private final String secret;
    // Sending happens off the request thread so a slow SMTP server never slows the site down.
    private final ExecutorService outbox = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mail-outbox");
        thread.setDaemon(true);
        return thread;
    });

    public NotificationService(MailService mail, UserRepository users, TrackRepository tracks,
                               @Value("${oskarro.app.jwtSecret}") String secret) {
        this.mail = mail;
        this.users = users;
        this.tracks = tracks;
        this.secret = secret;
    }

    public void commented(Track track, User commenter, String text) {
        User author = track.getAuthor();
        if (!wantsActivity(author, commenter)) {
            return;
        }
        String quote = text.length() > 300 ? text.substring(0, 300) + "…" : text;
        send(author, Kind.ACTIVITY, commenter.getUsername() + " skomentował Twój utwór „" + track.getTitle() + "”",
                commenter.getUsername() + " zostawił komentarz pod Twoim utworem „" + label(track) + "”:\n\n"
                        + "„" + quote + "”\n\n"
                        + "Odpowiedz albo zobacz utwór: " + SITE + "/track/" + track.getId());
    }

    public void liked(Track track, User voter) {
        User author = track.getAuthor();
        if (!wantsActivity(author, voter)) {
            return;
        }
        Instant now = Instant.now();
        if (author.getLastLikeMailAt() != null && author.getLastLikeMailAt().plus(LIKE_PAUSE).isAfter(now)) {
            return;
        }
        author.setLastLikeMailAt(now);
        users.save(author);
        send(author, Kind.ACTIVITY, "Ktoś polubił Twój utwór „" + track.getTitle() + "”",
                voter.getUsername() + " dał +1 Twojemu utworowi „" + label(track) + "”.\n\n"
                        + "Zobacz, jak mu idzie: " + SITE + "/track/" + track.getId() + "\n\n"
                        + "(Takie powiadomienie wysyłamy najwyżej raz na 12 godzin.)");
    }

    /** Mondays at 10:00 Warsaw time: new tracks of the past week in each opted-in user's favourite genres. */
    @Scheduled(cron = "0 0 10 * * MON", zone = "Europe/Warsaw")
    public void weeklyDigest() {
        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<Track> fresh = tracks.findAll().stream()
                .filter(track -> track.getCreatedAt() != null && track.getCreatedAt().isAfter(since))
                .filter(track -> track.getUrl() != null && !track.getUrl().isBlank())
                .sorted(Comparator.comparing(Track::getPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Track::getId, Comparator.reverseOrder()))
                .toList();
        if (fresh.isEmpty()) {
            return;
        }
        for (User user : users.findAll()) {
            if (!Boolean.TRUE.equals(user.getDigestOptIn()) || !reachable(user)) {
                continue;
            }
            Set<String> genres = genres(user.getFavoriteGenres());
            List<Track> picks = fresh.stream()
                    .filter(track -> track.getAuthor() == null || !track.getAuthor().getId().equals(user.getId()))
                    .filter(track -> genres.isEmpty() || genres.contains(String.valueOf(track.getGenre()).toUpperCase(Locale.ROOT)))
                    .limit(8)
                    .toList();
            if (picks.isEmpty()) {
                continue;
            }
            StringBuilder body = new StringBuilder(genres.isEmpty()
                    ? "Nowości z ostatniego tygodnia na VIXA:\n\n"
                    : "Nowości z ostatniego tygodnia w Twoich ulubionych gatunkach:\n\n");
            for (Track track : picks) {
                body.append("• ").append(label(track)).append("\n  ").append(SITE).append("/track/").append(track.getId()).append("\n");
            }
            body.append("\nWłącz radio i słuchaj z nami: ").append(SITE).append("/radio");
            send(user, Kind.DIGEST, "Nowości tygodnia na VIXA", body.toString());
        }
    }

    /** True when the link really comes from us; then the user is switched off that kind of mail. */
    public boolean unsubscribe(Integer userId, String kind, String token) {
        Kind which;
        try {
            which = Kind.valueOf(String.valueOf(kind).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (userId == null || token == null
                || !MessageDigest.isEqual(sign(userId, which).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        Optional<User> user = users.findById(userId);
        if (user.isEmpty()) {
            return false;
        }
        if (which == Kind.ACTIVITY) {
            user.get().setNotifyActivity(false);
        } else {
            user.get().setDigestOptIn(false);
        }
        users.save(user.get());
        return true;
    }

    String unsubscribeLink(User user, Kind kind) {
        return SITE + "/api/notify/unsubscribe?u=" + user.getId() + "&k=" + kind.name().toLowerCase(Locale.ROOT)
                + "&t=" + sign(user.getId(), kind);
    }

    private void send(User to, Kind kind, String subject, String text) {
        String footer = "\n\n—\nVIXA · " + SITE + "\n"
                + (kind == Kind.ACTIVITY
                ? "Nie chcesz powiadomień o komentarzach i polubieniach? Wypisz się jednym kliknięciem: "
                : "Nie chcesz cotygodniowych nowości? Wypisz się jednym kliknięciem: ")
                + unsubscribeLink(to, kind) + "\nMożesz to też zmienić w swoim profilu.";
        String address = to.getEmail();
        outbox.submit(() -> mail.send(address, subject, "Cześć " + to.getUsername() + "!\n\n" + text + footer));
    }

    private boolean wantsActivity(User author, User actor) {
        return author != null && actor != null && !author.getId().equals(actor.getId())
                && !Boolean.FALSE.equals(author.getNotifyActivity()) && reachable(author);
    }

    private static boolean reachable(User user) {
        return user.getEmail() != null && !user.getEmail().isBlank()
                && !Boolean.FALSE.equals(user.getActivated()) && !Boolean.TRUE.equals(user.getBanned());
    }

    private static String label(Track track) {
        String version = track.getVersion() == null || track.getVersion().isBlank() ? "" : " (" + track.getVersion().trim() + ")";
        return track.getArtist() + " – " + track.getTitle() + version;
    }

    private static Set<String> genres(String raw) {
        Set<String> out = new HashSet<>();
        for (String part : (raw == null ? "" : raw).split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private String sign(Integer userId, Kind kind) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(("unsubscribe:" + userId + ":" + kind.name()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
