package com.oskarro.oskarito.report;

import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
public class ReportService {

    static final Set<String> REASONS = Set.of("COPYRIGHT", "SPAM", "OFFENSIVE", "BROKEN_LINK", "OTHER");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ContentReportRepository reports;
    private final TrackRepository tracks;

    public ReportService(ContentReportRepository reports, TrackRepository tracks) {
        this.reports = reports;
        this.tracks = tracks;
    }

    public record NewReport(String reason, Integer trackId, String url, String details,
                            String name, String email, String website) {
    }

    public record ReportView(Long id, String reason, Integer trackId, String trackTitle, String url, String details,
                             String reporterName, String reporterEmail, String reporterUser, String status,
                             String resolution, String resolvedBy, String resolvedAt, String createdAt) {
    }

    public void submit(NewReport report) {
        if (report.website() != null && !report.website().isBlank()) {
            return; // honeypot filled in: a bot, pretend success
        }
        String reason = report.reason() == null ? "" : report.reason().trim().toUpperCase();
        if (!REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz powód zgłoszenia");
        }
        String details = trim(report.details());
        if (details.length() < 10 || details.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opisz sprawę (10–2000 znaków)");
        }
        String email = trim(report.email());
        if (!email.isEmpty() && (email.length() > 120 || !EMAIL.matcher(email).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny adres e-mail");
        }
        if ("COPYRIGHT".equals(reason) && email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Przy naruszeniu praw autorskich podaj e-mail do kontaktu");
        }
        if (report.trackId() != null && tracks.findById(report.trackId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie ma takiego utworu");
        }
        ContentReport entry = new ContentReport();
        entry.setReason(reason);
        entry.setTrackId(report.trackId());
        entry.setUrl(limit(trim(report.url()), 300));
        entry.setDetails(details);
        entry.setReporterName(limit(trim(report.name()), 100));
        entry.setReporterEmail(email.isEmpty() ? null : email);
        entry.setReporterUser(currentUsername());
        entry.setStatus("OPEN");
        entry.setCreatedAt(Instant.now());
        reports.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ReportView> list(String status) {
        List<ContentReport> rows = status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                ? reports.findAllByOrderByCreatedAtDesc()
                : reports.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        return rows.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return reports.countByStatus("OPEN");
    }

    public ReportView resolve(Long id, String status, String resolution, String admin) {
        ContentReport report = reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego zgłoszenia"));
        String next = status == null ? "RESOLVED" : status.trim().toUpperCase();
        if (!next.equals("OPEN") && !next.equals("RESOLVED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status: OPEN albo RESOLVED");
        }
        report.setStatus(next);
        report.setResolution(limit(trim(resolution), 500));
        report.setResolvedBy(next.equals("RESOLVED") ? admin : null);
        report.setResolvedAt(next.equals("RESOLVED") ? Instant.now() : null);
        return view(reports.save(report));
    }

    private ReportView view(ContentReport r) {
        String title = r.getTrackId() == null ? null
                : tracks.findById(r.getTrackId()).map(t -> t.getArtist() + " – " + t.getTitle()).orElse("(usunięty)");
        return new ReportView(r.getId(), r.getReason(), r.getTrackId(), title, r.getUrl(), r.getDetails(),
                r.getReporterName(), r.getReporterEmail(), r.getReporterUser(), r.getStatus(), r.getResolution(),
                r.getResolvedBy(), r.getResolvedAt() == null ? null : r.getResolvedAt().toString(), r.getCreatedAt().toString());
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated() ? null : auth.getName();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int max) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
