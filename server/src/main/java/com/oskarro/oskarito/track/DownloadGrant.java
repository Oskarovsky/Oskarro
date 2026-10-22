package com.oskarro.oskarito.track;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One activity that has already handed out download credits, so repeating it (e.g. unlike + like) gives nothing. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "download_grant", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kind", "item_key"}))
public class DownloadGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 16)
    private String kind;

    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;

    private Instant createdAt;
}
