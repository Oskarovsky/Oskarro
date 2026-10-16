package com.oskarro.oskarito.track;

import jakarta.annotation.PostConstruct;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class WaveformService {

    private static final int BARS = 180;
    private static final int MAX_BYTES = 30 * 1024 * 1024;

    private final Path directory = Paths.get("uploads", "peaks");
    private final HostujeAudioResolver hostujeAudioResolver;
    private final SoundfilesAudioResolver soundfilesAudioResolver;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private final Semaphore decodeSlots = new Semaphore(1);

    public WaveformService(HostujeAudioResolver hostujeAudioResolver,
                           SoundfilesAudioResolver soundfilesAudioResolver) {
        this.hostujeAudioResolver = hostujeAudioResolver;
        this.soundfilesAudioResolver = soundfilesAudioResolver;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(directory);
    }

    public Waveform peaks(String host, String id) {
        String src = audioUrl(host, id);
        assertRemoteAudio(src);
        String key = host + "-" + ("hostuje".equals(host) ? id.toLowerCase() : id);
        Path file = directory.resolve(key + ".json");
        Object lock = locks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            if (Files.isRegularFile(file)) {
                return read(file);
            }
            Waveform waveform;
            try {
                waveform = compute(src);
            } catch (RuntimeException ex) {
                waveform = drawn(key);
            }
            write(file, waveform);
            return waveform;
        }
    }

    private String audioUrl(String host, String id) {
        if ("hostuje".equals(host)) {
            return hostujeAudioResolver.resolve(id);
        }
        if ("soundfiles".equals(host)) {
            return soundfilesAudioResolver.resolve(id);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznane źródło pliku");
    }

    private void assertRemoteAudio(String src) {
        URI uri = URI.create(src);
        String remote = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = "https".equalsIgnoreCase(uri.getScheme()) && (
                remote.endsWith(".hostuje.org")
                        || remote.equals("soundfiles.eu")
                        || remote.endsWith(".soundfiles.eu")
                        || remote.endsWith(".r2.cloudflarestorage.com"));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
        }
    }

    private Waveform compute(String src) {
        try {
            decodeSlots.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(src))
                    .timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "Oskarro")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String type = response.headers().firstValue("content-type").orElse("");
            if (response.statusCode() / 100 != 2 || !type.toLowerCase(Locale.ROOT).startsWith("audio")) {
                response.body().close();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
            }
            try (InputStream body = new LimitedInputStream(response.body(), MAX_BYTES)) {
                return decode(body);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
        } finally {
            decodeSlots.release();
        }
    }

    private Waveform decode(InputStream input) throws Exception {
        Bitstream bitstream = new Bitstream(input);
        Decoder decoder = new Decoder();
        List<Float> frames = new ArrayList<>();
        double durationMs = 0;
        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                short[] pcm = samples.getBuffer();
                int length = Math.min(pcm.length, samples.getBufferLength());
                if (length <= 0) {
                    bitstream.closeFrame();
                    continue;
                }
                double sum = 0;
                for (int i = 0; i < length; i++) {
                    double value = pcm[i] / 32768.0;
                    sum += value * value;
                }
                frames.add((float) Math.sqrt(sum / length));
                durationMs += header.ms_per_frame();
                bitstream.closeFrame();
                if (frames.size() > 200_000) {
                    break;
                }
            }
        } finally {
            try {
                bitstream.close();
            } catch (Exception ignored) {
                // The file is already consumed.
            }
        }
        if (frames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
        }
        float[] peaks = shape(bucket(frames, BARS));
        double duration = Math.round(durationMs / 10.0) / 100.0;
        return new Waveform(duration, peaks);
    }

    private float[] bucket(List<Float> frames, int bars) {
        float[] peaks = new float[bars];
        int[] counts = new int[bars];
        for (int i = 0; i < frames.size(); i++) {
            int index = (int) ((long) i * bars / frames.size());
            if (index >= bars) {
                index = bars - 1;
            }
            peaks[index] += frames.get(i);
            counts[index]++;
        }
        for (int i = 0; i < bars; i++) {
            if (counts[i] > 0) {
                peaks[i] /= counts[i];
            }
        }
        return peaks;
    }

    private float[] shape(float[] peaks) {
        float loudest = 0;
        for (float peak : peaks) {
            loudest = Math.max(loudest, peak);
        }
        if (loudest <= 0) {
            return peaks;
        }
        for (int i = 0; i < peaks.length; i++) {
            peaks[i] = (float) Math.pow(peaks[i] / loudest, 0.72);
        }
        return peaks;
    }

    private Waveform drawn(String key) {
        long state = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < key.length(); i++) {
            state = state * 131 + key.charAt(i);
        }
        float[] peaks = new float[BARS];
        for (int i = 0; i < BARS; i++) {
            state = state * 6364136223846793005L + 1;
            double noise = ((state >>> 33) & 1023) / 1023.0;
            double t = i / (double) (BARS - 1);
            double env = 0.42 + 0.58 * Math.sin(Math.PI * Math.min(1, t * 1.04));
            env *= 0.78 + 0.22 * Math.sin(t * Math.PI * 5);
            peaks[i] = (float) Math.min(1, Math.max(0.12, env * (0.62 + 0.38 * noise)));
        }
        return new Waveform(0, peaks);
    }

    private void write(Path file, Waveform waveform) {
        StringBuilder json = new StringBuilder();
        json.append("{\"duration\":").append(waveform.duration()).append(",\"peaks\":[");
        float[] peaks = waveform.peaks();
        for (int i = 0; i < peaks.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(String.format(Locale.US, "%.4f", peaks[i]));
        }
        json.append("]}");
        try {
            Files.writeString(file, json.toString());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się zapisać fali dźwięku");
        }
    }

    private Waveform read(Path file) {
        try {
            String text = Files.readString(file);
            int durationAt = text.indexOf("\"duration\":");
            int peaksAt = text.indexOf("\"peaks\":[");
            int peaksEnd = text.lastIndexOf(']');
            if (durationAt < 0 || peaksAt < 0 || peaksEnd < peaksAt) {
                throw new IOException("bad peaks");
            }
            double duration = Double.parseDouble(text.substring(durationAt + 11, text.indexOf(',', durationAt)).trim());
            String body = text.substring(peaksAt + 9, peaksEnd).trim();
            if (body.isEmpty()) {
                return new Waveform(duration, new float[0]);
            }
            String[] parts = body.split(",");
            float[] peaks = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                peaks[i] = Float.parseFloat(parts[i].trim());
            }
            return new Waveform(duration, peaks);
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // The next request will compute the waveform again.
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się odczytać fali dźwięku");
        }
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private long left;

        private LimitedInputStream(InputStream in, long max) {
            super(in);
            this.left = max;
        }

        @Override
        public int read() throws IOException {
            if (left <= 0) {
                throw new IOException("audio too large");
            }
            int value = super.read();
            if (value >= 0) {
                left--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (left <= 0) {
                throw new IOException("audio too large");
            }
            int read = super.read(buffer, offset, (int) Math.min(length, left));
            if (read > 0) {
                left -= read;
            }
            return read;
        }
    }
}
