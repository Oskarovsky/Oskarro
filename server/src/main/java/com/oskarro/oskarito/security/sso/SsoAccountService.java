package com.oskarro.oskarito.security.sso;

import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Finds or creates the local account behind a Google/Facebook sign-in. */
@Service
public class SsoAccountService {

    private static final Logger log = LoggerFactory.getLogger(SsoAccountService.class);

    /** Why a sign-in was refused; the frontend shows a matching message. */
    public static class SsoRefused extends RuntimeException {
        private final String code;

        public SsoRefused(String code) {
            super(code);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public SsoAccountService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional
    public User accountFor(String provider, Map<String, Object> attributes) {
        String email = text(attributes.get("email")).toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            throw new SsoRefused("brak-email");
        }
        if ("google".equals(provider) && !Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw new SsoRefused("niepotwierdzony-email");
        }
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            user = users.findAll().stream()
                    .filter(candidate -> candidate.getEmail() != null && candidate.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);
        }
        if (user != null) {
            if (Boolean.TRUE.equals(user.getBanned())) {
                throw new SsoRefused("zablokowane");
            }
            // Someone signed up with this address but never proved it. The provider has now proven it,
            // so the account goes to its owner and the password set by whoever signed up stops working.
            boolean unproven = Boolean.FALSE.equals(user.getEmailVerified())
                    || (user.getEmailVerified() == null && Boolean.FALSE.equals(user.getActivated()));
            if (unproven) {
                log.warn("SSO {} took over unverified account id={}: password reset", provider, user.getId());
                user.setPassword(encoder.encode(UUID.randomUUID().toString()));
                user.setAuthProvider(provider);
            }
            user.setEmailVerified(true);
            // The provider has verified the address, so a pending activation is no longer needed.
            user.setActivated(true);
            users.save(user);
            // The caller builds the JWT from the roles after this transaction has closed.
            Hibernate.initialize(user.getRoles());
            return user;
        }
        Role userRole = roles.findByName(RoleName.ROLE_USER).orElseThrow();
        Instant now = Instant.now();
        User created = User.builder()
                .username(freeUsername(text(attributes.get("name")), email))
                .email(email)
                .password(encoder.encode(UUID.randomUUID().toString()))
                .activated(true)
                .emailVerified(true)
                .createdAt(now)
                .termsAcceptedAt(now)
                .authProvider(provider)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        return users.save(created);
    }

    /** Builds a username matching the signup rules ([letters digits _ . -], 3–32) and not taken yet. */
    String freeUsername(String name, String email) {
        String base = clean(name);
        if (base.length() < 3) {
            base = clean(email.substring(0, email.indexOf('@')));
        }
        if (base.length() < 3) {
            base = "dj" + base;
        }
        base = base.length() > 26 ? base.substring(0, 26) : base;
        String candidate = base;
        for (int i = 2; users.existsByUsername(candidate); i++) {
            candidate = base + i;
        }
        return candidate;
    }

    private static String clean(String raw) {
        String normalized = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFKC).trim().replaceAll("\\s+", ".");
        return normalized.replaceAll("[^\\p{L}0-9_.-]", "").replaceAll("\\.{2,}", ".").replaceAll("^[.-]+|[.-]+$", "");
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
