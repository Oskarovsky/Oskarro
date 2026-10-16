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
import java.util.regex.Pattern;

@Service
public class SoundfilesAudioResolver {

    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9]{12}$");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public String resolve(String rawId) {
        String id = rawId == null ? "" : rawId.trim();
        if (!ID.matcher(id).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid soundfiles id");
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
                    .uri(URI.create("https://soundfiles.eu/" + id + "/file/stream"))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Oskarro")
                    .GET()
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Soundfiles player unavailable");
            }
            String location = response.headers().firstValue("location")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Soundfiles audio missing"));
            URI target = URI.create(location);
            String host = target.getHost() == null ? "" : target.getHost().toLowerCase();
            if (!"https".equalsIgnoreCase(target.getScheme())
                    || !(host.endsWith(".soundfiles.eu") || host.equals("soundfiles.eu") || host.endsWith(".r2.cloudflarestorage.com"))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Soundfiles audio missing");
            }
            return location;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Soundfiles audio missing");
        }
    }

    private record Cached(String src, Instant until) {
    }
}
