package com.oskarro.oskarito.track;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A track the user has spent a credit on; downloading it again is free. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "download_unlock", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "track_id"}))
public class DownloadUnlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "track_id", nullable = false)
    private Integer trackId;

    private Instant createdAt;
}
