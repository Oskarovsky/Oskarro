package com.oskarro.oskarito.track;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadGrantRepository extends JpaRepository<DownloadGrant, Integer> {
    boolean existsByUserIdAndKindAndItemKey(Integer userId, String kind, String itemKey);
}
