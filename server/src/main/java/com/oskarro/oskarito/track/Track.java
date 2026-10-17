package com.oskarro.oskarito.track;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.record.Record;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.video.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private String artist;

    private Integer points;

    private String genre;

    private String version;

    private String url;

    private Instant createdAt;

    private Integer position;

    /** "set" for DJ sets and mixes; null means a single track. */
    @Column(length = 10)
    private String kind;

    /** For sets: one of {@link SetCategory}. */
    @Column(length = 20)
    private String setCategory;

    /** For sets: club, festival or radio show name (optional). */
    @Column(length = 120)
    private String eventName;

    /** For sets: free-text tracklist, one track per line (optional). */
    @Column(length = 5000)
    private String tracklist;


    @ManyToOne
    @JoinColumn(name = "record_id")
    private Record record;

    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "video_id")
    private Video video;

    @ManyToOne
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private User author;

    @Transient
    private String sharedBy;

    @Transient
    private String authorRank;

    @Transient
    private Integer likeCount;

    @Transient
    private Integer dislikeCount;

    @Transient
    private Integer score;

    @Transient
    private Integer viewsMonth;

    @Transient
    private Integer viewsYear;

    @Transient
    private Integer viewsAll;

    @Transient
    private Integer commentCount;

    @Transient
    private Boolean hasCover;

    @Transient
    private String downloadUrl;

    @Transient
    private Integer downloadSlots;

    @Transient
    private Boolean editable;

    @Transient
    private String editableUntil;

}

