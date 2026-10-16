package com.oskarro.oskarito.track;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class TrackFileService {

    private static final int SAMPLE_BYTES = 2_500_000;
    private static final double SAMPLE_MS = 48_000;

    private final TrackRepository tracks;
    private final TrackFileRepository files;
    private final HostujeAudioResolver hostujeAudioResolver;
    private final SoundfilesAudioResolver soundfilesAudioResolver;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(1);

    public TrackFileService(TrackRepository tracks,
                            TrackFileRepository files,
                            HostujeAudioResolver hostujeAudioResolver,
                            SoundfilesAudioResolver soundfilesAudioResolver) {
        this.tracks = tracks;
        this.files = files;
        this.hostujeAudioResolver = hostujeAudioResolver;
        this.soundfilesAudioResolver = soundfilesAudioResolver;
    }

    public TrackFileView facts(Integer trackId) {
        Track track = tracks.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego utworu"));
        String source = TrackSocialService.canonical(track.getUrl());
        if (source == null) {
            return TrackFileView.from(null);
        }
        Optional<TrackFile> saved = files.findById(trackId);
        if (saved.isPresent() && source.equals(saved.get().getSourceUrl())) {
            return TrackFileView.from(saved.get());
        }
        Object lock = locks.computeIfAbsent(trackId, ignored -> new Object());
        synchronized (lock) {
            saved = files.findById(trackId);
            if (saved.isPresent() && source.equals(saved.get().getSourceUrl())) {
                return TrackFileView.from(saved.get());
            }
            TrackFile probed = probe(trackId, source);
            if (probed == null) {
                return TrackFileView.from(null);
            }
            return TrackFileView.from(files.save(probed));
        }
    }

    private TrackFile probe(Integer trackId, String pageUrl) {
        String src = streamUrl(pageUrl);
        if (src == null || !allowed(src)) {
            return null;
        }
        try {
            slots.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(src))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", "Oskarro")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                response.body().close();
                return null;
            }
            long size = response.headers().firstValueAsLong("content-length").orElse(-1);
            byte[] sample;
            try (InputStream body = response.body()) {
                sample = body.readNBytes(SAMPLE_BYTES);
            }
            TrackFile file = read(trackId, pageUrl, size > 0 ? size : null, sample);
            return file.getFormat() == null && file.getBytes() == null ? null : file;
        } catch (Exception ex) {
            return null;
        } finally {
            slots.release();
        }
    }

    private TrackFile read(Integer trackId, String sourceUrl, Long bytes, byte[] sample) {
        TrackFile file = new TrackFile();
        file.setTrackId(trackId);
        file.setSourceUrl(sourceUrl);
        file.setBytes(bytes);
        if (sample.length >= 4 && sample[0] == 'f' && sample[1] == 'L' && sample[2] == 'a' && sample[3] == 'C') {
            file.setFormat("FLAC");
            readFlac(file, sample);
            return file;
        }
        readMp3(file, sample);
        return file;
    }

    private void readFlac(TrackFile file, byte[] sample) {
        if (sample.length < 26) {
            return;
        }
        int blockType = sample[4] & 0x7f;
        if (blockType != 0) {
            return;
        }
        int sampleRate = readBits(sample, 8, 80, 20);
        int channels = readBits(sample, 8, 100, 3) + 1;
        int depth = readBits(sample, 8, 103, 5) + 1;
        if (sampleRate > 0) {
            file.setSampleRate(sampleRate);
        }
        if (channels == 1 || channels == 2) {
            file.setChannels(channels);
        }
        if (depth >= 8 && depth <= 32) {
            file.setBitDepth(depth);
        }
    }

    private static int readBits(byte[] data, int byteOffset, int bitOffset, int width) {
        int start = byteOffset * 8 + bitOffset;
        int value = 0;
        for (int i = 0; i < width; i++) {
            int pos = start + i;
            int current = data[pos / 8] & 0xff;
            int bit = (current >> (7 - (pos % 8))) & 1;
            value = (value << 1) | bit;
        }
        return value;
    }

    private void readMp3(TrackFile file, byte[] sample) {
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(sample));
        Decoder decoder = new Decoder();
        List<Double> energy = new ArrayList<>();
        double elapsed = 0;
        int bitrate = 0;
        int varied = 0;
        try {
            Header header;
            while (elapsed < SAMPLE_MS && (header = bitstream.readFrame()) != null) {
                if (file.getSampleRate() == null && header.frequency() > 0) {
                    file.setSampleRate(header.frequency());
                    file.setChannels(header.mode() == 3 ? 1 : 2);
                }
                int rate = header.bitrate();
                if (rate > 2000) {
                    rate = rate / 1000;
                }
                if (rate > 0) {
                    if (bitrate == 0) {
                        bitrate = rate;
                    } else if (rate != bitrate) {
                        varied++;
                    }
                }
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                short[] pcm = samples.getBuffer();
                int length = Math.min(pcm.length, samples.getBufferLength());
                if (length > 0) {
                    double sum = 0;
                    for (int i = 0; i < length; i++) {
                        double value = pcm[i] / 32768.0;
                        sum += value * value;
                    }
                    energy.add(Math.sqrt(sum / length));
                }
                elapsed += header.ms_per_frame();
                bitstream.closeFrame();
            }
        } catch (Exception ignored) {
            // A short sample is enough. The rest of the file stays on the host.
        } finally {
            try {
                bitstream.close();
            } catch (Exception ignored) {
                // Already consumed.
            }
        }
        if (bitrate > 0) {
            file.setBitrate(bitrate);
            file.setFormat(varied > 8 ? "VBR" : "MP3");
        }
        if (energy.size() > 40 && elapsed > 8000) {
            file.setBpm(tempo(energy, elapsed / energy.size()));
        }
    }

    private Integer tempo(List<Double> energy, double hopMs) {
        double[] flux = new double[energy.size()];
        for (int i = 1; i < energy.size(); i++) {
            double diff = energy.get(i) - energy.get(i - 1);
            flux[i] = Math.max(0, diff);
        }
        int minLag = Math.max(1, (int) Math.round(60000.0 / 180.0 / hopMs));
        int maxLag = Math.min(flux.length / 2, (int) Math.round(60000.0 / 80.0 / hopMs));
        if (maxLag <= minLag) {
            return null;
        }
        double best = 0;
        int bestLag = minLag;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double sum = 0;
            int last = flux.length - lag;
            for (int i = 0; i < last; i++) {
                sum += flux[i] * flux[i + lag];
            }
            if (sum > best) {
                best = sum;
                bestLag = lag;
            }
        }
        if (best <= 0) {
            return null;
        }
        double bpm = 60000.0 / (bestLag * hopMs);
        if (bpm < 95) {
            bpm *= 2;
        } else if (bpm > 175) {
            bpm /= 2;
        }
        int rounded = (int) Math.round(bpm);
        return rounded >= 80 && rounded <= 180 ? rounded : null;
    }

    private String streamUrl(String pageUrl) {
        try {
            String hostuje = TrackSocialService.hostujeFile(pageUrl);
            if (hostuje != null) {
                int mark = hostuje.lastIndexOf('=');
                return hostujeAudioResolver.resolve(hostuje.substring(mark + 1));
            }
            String id = TrackSocialService.soundfilesId(pageUrl);
            return id == null ? null : soundfilesAudioResolver.resolve(id);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean allowed(String src) {
        URI uri = URI.create(src);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return "https".equalsIgnoreCase(uri.getScheme()) && (
                host.endsWith(".hostuje.org")
                        || host.equals("soundfiles.eu")
                        || host.endsWith(".soundfiles.eu")
                        || host.endsWith(".r2.cloudflarestorage.com"));
    }
}
