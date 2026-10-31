package com.oskarro.oskarito.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users,
                                PasswordResetRepository resets,
                                PasswordEncoder encoder,
                                MailService mail) {
        this.users = users;
        this.resets = resets;
        this.encoder = encoder;
        this.mail = mail;
    }

    @Transactional
    public boolean request(String email) {
        String address = email == null ? "" : email.trim();
        return users.findByEmail(address)
                .map(this::issueAndSend)
                .orElse(false);
    }

    private boolean issueAndSend(User user) {
        resets.deleteByUserId(user.getId());
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        resets.save(new PasswordReset(token, user.getId(), Instant.now().plus(2, ChronoUnit.HOURS)));
        String link = "https://oskarro.com/haslo?token=" + token;
        String body = """
                Cześć,

                Otwórz ten link, żeby ustawić nowe hasło w serwisie VIXA:
                %s

                Link działa dwie godziny.

                Hello,
                Open this link to set a new password on VIXA:
                %s
                The link works for two hours.
                """.formatted(link, link);
        return mail.send(user.getEmail(), "Nowe hasło w serwisie VIXA", body);
    }

    @Transactional
    public void reset(String token, String password) {
        String fresh = password == null ? "" : password;
        if (fresh.length() < 6 || fresh.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło musi mieć od 6 do 40 znaków");
        }
        PasswordReset note = resets.findById(token == null ? "" : token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link jest nieważny"));
        if (note.getExpiresAt() == null || note.getExpiresAt().isBefore(Instant.now())) {
            resets.delete(note);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link wygasł");
        }
        User user = users.findById(note.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link jest nieważny"));
        user.setPassword(encoder.encode(fresh));
        users.save(user);
        resets.delete(note);
    }
}
