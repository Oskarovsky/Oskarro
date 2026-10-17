package com.oskarro.oskarito.admin;

import java.util.List;

/** Response shapes for the admin API. */
public final class AdminViews {

    private AdminViews() {
    }

    public record Page<T>(List<T> items, int page, int size, long total) {
    }

    public record Overview(long users, long newUsers7d, long newUsers30d,
                           long active24h, long active7d, long active30d,
                           long notActivated, long banned, long staff,
                           long tracks, long tracks7d, long comments, long comments7d,
                           long votes, long plays, long plays7d,
                           List<UserRow> latestUsers, List<Event> latestEvents) {
    }

    public record UserRow(Integer id, String username, String email, String createdAt,
                          String lastLoginAt, String lastSeenAt, boolean activated, boolean banned,
                          String banReason, List<String> roles, String rankOverride,
                          long tracks, long comments, long votes) {
    }

    public record UserDetail(UserRow user, String bio, String website, List<TrackRow> tracks,
                             List<CommentRow> comments, List<VoteRow> votes, List<AuditRow> audit) {
    }

    public record TrackRow(Integer id, String title, String artist, String version, String genre,
                           String url, String author, String createdAt, int score, int views, int comments) {
    }

    public record CommentRow(Integer id, String text, String author, Integer trackId, String trackTitle,
                             String createdAt) {
    }

    public record VoteRow(Integer trackId, String trackTitle, int vote, String createdAt) {
    }

    public record Event(String type, String at, String actor, String text, String targetType, Integer targetId) {
    }

    public record AuditRow(Long id, String actor, String action, String targetType, Integer targetId,
                           String targetLabel, String details, String createdAt) {
    }

    /** Fields an admin may change on an account; null means "leave as is". */
    public record UserChange(Boolean activated, Boolean banned, String banReason, List<String> roles,
                             String rankOverride) {
    }

    public record TrackChange(String artist, String title, String version, String genre, String url) {
    }

    public record CommentChange(String text) {
    }
}
