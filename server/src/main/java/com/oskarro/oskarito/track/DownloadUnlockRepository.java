package com.oskarro.oskarito.track;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DownloadUnlockRepository extends JpaRepository<DownloadUnlock, Integer> {
    boolean existsByUserIdAndTrackId(Integer userId, Integer trackId);

    @Query("select u.trackId from DownloadUnlock u where u.userId = :userId and u.trackId in :ids")
    List<Integer> unlockedAmong(@Param("userId") Integer userId, @Param("ids") Collection<Integer> ids);
}
