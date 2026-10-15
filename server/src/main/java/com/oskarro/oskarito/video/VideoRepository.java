package com.oskarro.oskarito.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<com.oskarro.oskarito.video.Video, Integer> {

    List<com.oskarro.oskarito.video.Video> findAll();

    List<com.oskarro.oskarito.video.Video> findVideosByCategory(String category);

    Optional<com.oskarro.oskarito.video.Video> findById(Integer id);

    com.oskarro.oskarito.video.Video save(com.oskarro.oskarito.video.Video video);

    void deleteById(Integer id);
}
