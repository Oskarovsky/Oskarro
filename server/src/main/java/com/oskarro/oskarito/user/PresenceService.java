package com.oskarro.oskarito.user;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Records when signed-in users were last active, writing to the database at most every few minutes per user. */
@Service
public class PresenceService {

    private static final Duration WRITE_EVERY = Duration.ofMinutes(5);

    private final UserRepository users;
    private final Map<Integer, Instant> lastWrite = new ConcurrentHashMap<>();

    public PresenceService(UserRepository users) {
        this.users = users;
    }

    public void seen(Integer userId) {
        Instant now = Instant.now();
        Instant previous = lastWrite.get(userId);
        if (previous != null && previous.plus(WRITE_EVERY).isAfter(now)) {
            return;
        }
        lastWrite.put(userId, now);
        users.markSeen(userId, now);
    }

    public void loggedIn(Integer userId) {
        Instant now = Instant.now();
        lastWrite.put(userId, now);
        users.markLogin(userId, now);
    }
}
