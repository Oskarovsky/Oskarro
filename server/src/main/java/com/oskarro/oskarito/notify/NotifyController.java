package com.oskarro.oskarito.notify;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The one-click unsubscribe link from notification mails. Answers with a tiny page, no login needed. */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final NotificationService notifications;

    public NotifyController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> unsubscribe(@RequestParam(required = false) Integer u,
                                       @RequestParam(required = false) String k,
                                       @RequestParam(required = false) String t) {
        boolean done = notifications.unsubscribe(u, k, t);
        String message = done
                ? ("digest".equalsIgnoreCase(k) ? "Nie będziemy już wysyłać cotygodniowych nowości." : "Nie będziemy już wysyłać powiadomień o komentarzach i polubieniach.")
                : "Ten link jest nieprawidłowy. Ustawienia maili zmienisz w swoim profilu.";
        String page = "<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\"><meta name=\"robots\" content=\"noindex\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Powiadomienia | VIXA</title></head>"
                + "<body style=\"font-family:system-ui,sans-serif;background:#12110e;color:#f6f1e6;display:grid;place-items:center;min-height:90vh;margin:0\">"
                + "<main style=\"max-width:420px;padding:24px;text-align:center\"><h1 style=\"font-size:1.5rem\">" + (done ? "Gotowe" : "Coś nie tak") + "</h1>"
                + "<p>" + message + "</p><p><a style=\"color:#e4ff4a\" href=\"/\">Wróć na VIXA</a></p></main></body></html>";
        return ResponseEntity.status(done ? 200 : 400).body(page);
    }
}
