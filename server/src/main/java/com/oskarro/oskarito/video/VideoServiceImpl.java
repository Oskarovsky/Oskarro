package com.oskarro.oskarito.video;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VideoServiceImpl implements com.oskarro.oskarito.video.VideoService {

    com.oskarro.oskarito.video.VideoRepository videoRepository;

    public VideoServiceImpl(com.oskarro.oskarito.video.VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Override
    public List<com.oskarro.oskarito.video.Video> getAllVideo() {
        return videoRepository.findAll();
    }

    @Override
    public List<com.oskarro.oskarito.video.Video> findVideosByCategory(String category) {
        return videoRepository.findVideosByCategory(category);
    }

    @Override
    public void addVideo(com.oskarro.oskarito.video.Video video) {
        videoRepository.save(video);
    }

    @Override
    public Optional<com.oskarro.oskarito.video.Video> findVideoById(Integer id) {
        return videoRepository.findById(id);
    }

    @Override
    public void deleteVideoById(Integer id) {
        videoRepository.deleteById(id);
    }
}
