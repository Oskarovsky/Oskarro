package com.oskarro.oskarito.track;

public record TrackFileView(
        Long bytes,
        Integer bitrate,
        Integer sampleRate,
        Integer channels,
        Integer bitDepth,
        Integer bpm,
        String format
) {
    static TrackFileView from(TrackFile file) {
        if (file == null) {
            return new TrackFileView(null, null, null, null, null, null, null);
        }
        Integer bitrate = file.getBitrate();
        if (bitrate != null && bitrate > 2000) {
            bitrate = bitrate / 1000;
        }
        return new TrackFileView(
                file.getBytes(),
                bitrate,
                file.getSampleRate(),
                file.getChannels(),
                file.getBitDepth(),
                file.getBpm(),
                file.getFormat());
    }
}
