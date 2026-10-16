package com.oskarro.oskarito.track;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TrackViewRepository extends CrudRepository<TrackView, Long> {

    @Query("select v.trackId, count(v) from TrackView v where v.trackId in :ids group by v.trackId")
    List<Object[]> countsAll(@Param("ids") Collection<Integer> ids);

    @Query("select v.trackId, count(v) from TrackView v where v.trackId in :ids and v.playedAt >= :since group by v.trackId")
    List<Object[]> countsSince(@Param("ids") Collection<Integer> ids, @Param("since") Instant since);

    @Modifying
    @Query("delete from TrackView v where v.trackId = :trackId")
    void deleteByTrackId(@Param("trackId") Integer trackId);
}
