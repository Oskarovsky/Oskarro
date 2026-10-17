package com.oskarro.oskarito.track;

import com.oskarro.oskarito.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"track_id", "user_id"}))
public class TrackLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 1 = like, -1 = dislike. Older rows may be null and count as a like. */
    private Integer vote;

    /** When the vote was cast; null for votes from before 2026-10-17. */
    private Instant createdAt;
}
