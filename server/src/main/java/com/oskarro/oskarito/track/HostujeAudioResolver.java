package com.oskarro.oskarito.track;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HostujeAudioResolver {

    private static final Pattern HASH = Pattern.compile("^[a-f0-9]{32}$");
    private static final Duration TTL = Duration.ofMinutes(15);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public String resolve(String hash) {
        String id = hash == null ? "" : hash.toLowerCase();
        if (!HASH.matcher(id).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hostuje id");
        }
        Cached cached = cache.get(id);
        if (cached != null && cached.until.isAfter(Instant.now())) {
            return cached.src;
        }
        String src = fetch(id);
        cache.put(id, new Cached(src, Instant.now().plus(TTL)));
        return src;
    }

    private String fetch(String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://hostuje.net/player_embed.js.php?id=" + id))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Oskarro")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Hostuje player unavailable");
            }
            Matcher matcher = Pattern.compile(
                    "https://b\\d+\\.hostuje\\.org/_tmp/id-" + id + "-p-1\\.mp3").matcher(response.body());
            if (!matcher.find()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Hostuje audio missing");
            }
            return matcher.group();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Hostuje audio missing");
        }
    }

    private record Cached(String src, Instant until) {
    }
}
