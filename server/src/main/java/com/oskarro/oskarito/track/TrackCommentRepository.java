package com.oskarro.oskarito.track;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TrackCommentRepository extends CrudRepository<TrackComment, Integer> {
    List<TrackComment> findByTrackIdOrderByCreatedAtAsc(Integer trackId);
    long countByAuthorId(Integer authorId);
    long countByTrack_Author_Id(Integer authorId);

    @Query("select c.author.id, count(c) from TrackComment c group by c.author.id")
    List<Object[]> countGroupedByAuthor();

    @Query("select c.track.author.id, count(c) from TrackComment c where c.track.author is not null group by c.track.author.id")
    List<Object[]> countGroupedByTrackAuthor();

    @Query("select c.track.id, count(c) from TrackComment c where c.track.id in :ids group by c.track.id")
    List<Object[]> countsForTracks(@Param("ids") Collection<Integer> ids);

    @Modifying
    @Query("delete from TrackComment c where c.track.id = :trackId")
    void deleteByTrackId(@Param("trackId") Integer trackId);

    @Query("select c.createdAt from TrackComment c where c.createdAt >= :from")
    List<Instant> createdSince(@Param("from") Instant from);
}
