package com.oskarro.oskarito.like;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A like on something other than a track: a playlist or a YouTube set. Tracks keep their own +1/−1 votes. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "content_like", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kind", "item_key"}))
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /** "playlist" or "video". */
    @Column(nullable = false, length = 16)
    private String kind;

    /** Playlist id or YouTube video id. */
    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;

    private Instant createdAt;
}
