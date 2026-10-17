package com.oskarro.oskarito.track;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Sub-sections of "Sety/Mixy". The slug is what the site uses in URLs. */
public enum SetCategory {
    USERS("uzytkownicy"),
    CLUBS("kluby"),
    FESTIVALS("festiwale"),
    RADIO("audycje");

    private final String slug;

    SetCategory(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    /** Accepts the enum name or the URL slug, case-insensitively. */
    public static Optional<SetCategory> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.slug.equals(key) || category.name().toLowerCase(Locale.ROOT).equals(key))
                .findFirst();
    }
}
