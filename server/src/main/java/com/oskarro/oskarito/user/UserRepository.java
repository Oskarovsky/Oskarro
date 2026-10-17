package com.oskarro.oskarito.user;

import java.util.List;
import java.time.Instant;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("select u.createdAt from User u where u.createdAt >= :from")
    List<Instant> registeredSince(@Param("from") Instant from);

    @Modifying
    @Transactional
    @Query("update User u set u.lastSeenAt = :at where u.id = :id")
    void markSeen(@Param("id") Integer id, @Param("at") Instant at);

    @Modifying
    @Transactional
    @Query("update User u set u.lastLoginAt = :at, u.lastSeenAt = :at where u.id = :id")
    void markLogin(@Param("id") Integer id, @Param("at") Instant at);
}

