package com.oskarro.oskarito.like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentLikeRepository extends JpaRepository<ContentLike, Integer> {
    long countByKindAndItemKey(String kind, String itemKey);

    Optional<ContentLike> findByUserIdAndKindAndItemKey(Integer userId, String kind, String itemKey);
}
