package com.oskarro.oskarito.track;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "track_file")
public class TrackFile {

    @Id
    private Integer trackId;

    private String sourceUrl;

    private Long bytes;

    private Integer bitrate;

    private Integer sampleRate;

    private Integer channels;

    private Integer bitDepth;

    private Integer bpm;

    private String format;
}
