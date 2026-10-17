package com.oskarro.oskarito.track;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrackLikeRepository extends JpaRepository<TrackLike, Integer> {
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

    @Modifying
    @Query("delete from TrackLike l where l.track.id = :trackId")
    void deleteByTrackId(@Param("trackId") Integer trackId);

    @Query("select l.createdAt from TrackLike l where l.createdAt >= :from")
    List<Instant> votedSince(@Param("from") Instant from);

    List<TrackLike> findTop200ByCreatedAtNotNullOrderByCreatedAtDesc();

    List<TrackLike> findTop20ByUser_IdOrderByIdDesc(Integer userId);

    @Query("select l.user.id, count(l) from TrackLike l group by l.user.id")
    List<Object[]> countGroupedByVoter();
}
