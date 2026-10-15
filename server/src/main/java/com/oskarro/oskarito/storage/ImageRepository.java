package com.oskarro.oskarito.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {

    Boolean existsByUserId(Integer userId);

    void deleteByUserId(Integer userId);

    Optional<Image> findByUserUsername(String username);

}
