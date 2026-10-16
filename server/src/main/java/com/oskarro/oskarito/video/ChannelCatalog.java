package com.oskarro.oskarito.video;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ChannelCatalog {

    static final String CHANNEL_ID = "UCyK2Ekq7NAs2x_QCfNMwDYg";
    private static final String UPLOADS = "UU" + CHANNEL_ID.substring(2);
    private static final Duration FRESH = Duration.ofHours(6);
    private static final Pattern CLOCK = Pattern.compile("^\\d{1,2}:\\d{2}(?::\\d{2})?$");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private volatile List<ChannelVideo> cached = List.of();
    private volatile Instant until = Instant.EPOCH;

    public List<ChannelVideo> videos() {
        if (Instant.now().isBefore(until) && !cached.isEmpty()) {
            return cached;
        }
        synchronized (this) {
            if (Instant.now().isBefore(until) && !cached.isEmpty()) {
                return cached;
            }
            try {
                List<ChannelVideo> fresh = fetch();
                cached = fresh;
                until = Instant.now().plus(FRESH);
            } catch (RuntimeException ex) {
                if (cached.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
                }
            }
            return cached;
        }
    }

    private List<ChannelVideo> fetch() {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.youtube.com/playlist?list=" + UPLOADS))
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept-Language", "pl")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
        }
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
        }
        String html = response.body();
        int marker = html.indexOf("ytInitialData");
        if (marker < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
        }
        int start = html.indexOf('{', marker);
        JSONObject data = new JSONObject(html.substring(start, matchingBrace(html, start)));
        Map<String, ChannelVideo> found = new LinkedHashMap<>();
        collect(data, found);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
        }
        return List.copyOf(found.values());
    }

    private static int matchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kanał Oskarro jest chwilowo niedostępny");
    }

    private void collect(Object node, Map<String, ChannelVideo> out) {
        if (node instanceof JSONObject obj) {
            Object lockup = obj.opt("lockupViewModel");
            if (lockup instanceof JSONObject view) {
                ChannelVideo video = fromLockup(view);
                if (video != null) {
                    out.putIfAbsent(video.id(), video);
                }
                return;
            }
            for (String key : obj.keySet()) {
                collect(obj.get(key), out);
            }
        } else if (node instanceof JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                collect(arr.get(i), out);
            }
        }
    }

    private ChannelVideo fromLockup(JSONObject lockup) {
        String id = lockup.optString("contentId", "");
        if (!id.matches("[A-Za-z0-9_-]{11}")) {
            return null;
        }
        JSONObject metadata = lockup.optJSONObject("metadata");
        String title = "";
        if (metadata != null) {
            JSONObject view = metadata.optJSONObject("lockupMetadataViewModel");
            if (view != null) {
                JSONObject titleNode = view.optJSONObject("title");
                if (titleNode != null) {
                    title = titleNode.optString("content", "").trim();
                }
            }
        }
        if (title.isEmpty()) {
            return null;
        }
        return new ChannelVideo(
                id,
                title,
                findDuration(lockup.optJSONObject("contentImage")),
                findPublished(metadata),
                findViews(metadata));
    }

    private String findViews(JSONObject metadata) {
        return firstLabel(metadata, "wyświetle");
    }

    private String firstLabel(Object node, String needle) {
        if (node instanceof JSONObject obj) {
            Object value = obj.opt("accessibilityLabel");
            if (value instanceof String text) {
                String trimmed = text.replace('\u00a0', ' ').trim();
                if (trimmed.toLowerCase().contains(needle)) {
                    return trimmed;
                }
            }
            for (String key : obj.keySet()) {
                String found = firstLabel(obj.get(key), needle);
                if (!found.isEmpty()) {
                    return found;
                }
            }
        } else if (node instanceof JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                String found = firstLabel(arr.get(i), needle);
                if (!found.isEmpty()) {
                    return found;
                }
            }
        }
        return "";
    }

    private String findDuration(JSONObject node) {
        List<String> texts = new ArrayList<>();
        collectTexts(node, texts);
        for (String text : texts) {
            String trimmed = text.replace('\u00a0', ' ').trim();
            if (CLOCK.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return "";
    }

    private String findPublished(JSONObject metadata) {
        List<String> texts = new ArrayList<>();
        collectTexts(metadata, texts);
        for (String text : texts) {
            String trimmed = text.replace('\u00a0', ' ').trim().toLowerCase();
            if (trimmed.endsWith("temu") || trimmed.equals("dzisiaj") || trimmed.equals("wczoraj")) {
                return text.replace('\u00a0', ' ').trim();
            }
        }
        return "";
    }

    private void collectTexts(Object node, List<String> out) {
        if (node instanceof JSONObject obj) {
            for (String key : List.of("content", "text")) {
                Object value = obj.opt(key);
                if (value instanceof String text && text.length() < 120) {
                    out.add(text);
                }
            }
            for (String key : obj.keySet()) {
                collectTexts(obj.get(key), out);
            }
        } else if (node instanceof JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                collectTexts(arr.get(i), out);
            }
        }
    }
}
