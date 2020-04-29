package com.oskarro.oskarito.video;

import java.util.List;
import java.util.Optional;

public interface VideoService {

    List<Video> getAllVideo();

    List<Video> findVideosByCategory(String category);

    void addVideo(Video video);

    Optional findVideoById(Integer id);

    void deleteVideoById(Integer id);
}
