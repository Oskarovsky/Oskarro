package com.oskarro.oskarito.track;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrackLikeRepository extends CrudRepository<TrackLike, Integer> {
    Optional<TrackLike> findByTrackIdAndUserId(Integer trackId, Integer userId);
    long countByUserId(Integer userId);

    @Query("select l.track.id, coalesce(l.vote, 1) from TrackLike l where l.track.id in :ids")
    List<Object[]> votesFor(@Param("ids") Collection<Integer> ids);

    @Query("select count(l) from TrackLike l where l.track.author.id = :authorId and coalesce(l.vote, 1) = 1")
    long countLikesReceived(@Param("authorId") Integer authorId);

    @Query("select count(l) from TrackLike l where l.track.author.id = :authorId and l.vote = -1")
    long countDislikesReceived(@Param("authorId") Integer authorId);

    @Query("select l.track.author.id, count(l) from TrackLike l where l.track.author is not null and coalesce(l.vote, 1) = 1 group by l.track.author.id")
    List<Object[]> likesGroupedByAuthor();

    @Query("select l.track.author.id, count(l) from TrackLike l where l.track.author is not null and l.vote = -1 group by l.track.author.id")
    List<Object[]> dislikesGroupedByAuthor();
}
