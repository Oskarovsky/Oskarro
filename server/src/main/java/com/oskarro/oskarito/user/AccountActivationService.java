package com.oskarro.oskarito.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class AccountActivationService {

    private final UserRepository users;
    private final AccountActivationRepository activations;
    private final MailService mail;
    private final SecureRandom random = new SecureRandom();

    public AccountActivationService(UserRepository users,
                                    AccountActivationRepository activations,
                                    MailService mail) {
        this.users = users;
        this.activations = activations;
        this.mail = mail;
    }

    @Transactional
    public boolean issueAndSend(User user) {
        activations.deleteByUserId(user.getId());
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        activations.save(new AccountActivation(token, user.getId(), Instant.now().plus(48, ChronoUnit.HOURS)));
        String link = "https://oskarro.com/aktywuj?token=" + token;
        String body = """
                Cześć,

                Założyłeś konto na oskarro.com. Otwórz ten link, żeby je włączyć:
                %s

                Link działa 48 godzin. Zanim go otworzysz, logowanie nie zadziała.

                Hello,
                Open this link to activate your oskarro.com account:
                %s
                The link works for 48 hours.
                """.formatted(link, link);
        return mail.send(user.getEmail(), "Aktywuj konto na oskarro.com", body);
    }

    @Transactional
    public void activate(String token) {
        AccountActivation note = activations.findById(token == null ? "" : token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link jest nieważny albo wygasł"));
        if (note.getExpiresAt() == null || note.getExpiresAt().isBefore(Instant.now())) {
            activations.delete(note);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link jest nieważny albo wygasł");
        }
        User user = users.findById(note.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link jest nieważny albo wygasł"));
        user.setActivated(true);
        user.setEmailVerified(true);
        users.save(user);
        activations.delete(note);
    }
}
