package com.oskarro.oskarito.track;

import com.oskarro.oskarito.playlist.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TrackServiceImpl implements TrackService {

    private final com.oskarro.oskarito.track.TrackRepository trackRepository;
    private final PlaylistRepository playlistRepository;

    public TrackServiceImpl(com.oskarro.oskarito.track.TrackRepository trackRepository, PlaylistRepository playlistRepository) {
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
    }
    @Override
    public List<Track> findAll() {
        return trackRepository.findAll();
    }

    @Override
    public Optional<Track> findById(Integer id) {
        return trackRepository.findById(id);
    }

    @Override
    public Track saveTrack(Track track) {
        return trackRepository.save(track);
    }

    @Override
    public List<Track> findByProviderId(Integer id) {
        return trackRepository.findTracksByProviderId(id);
    }

    @Override
    public List<Track> findTracksByGenre(String genre) {
        return trackRepository.findTracksByGenre(genre.toUpperCase());
    }

    @Override
    public List<Track> findByProviderIdAndGenre(Integer id, String genre) {
        return trackRepository.findTracksByProviderIdAndGenre(id, genre.toUpperCase());
    }

    @Override
    public List<Track> findTracksByProviderName(String name) {
        return trackRepository.findTracksByProviderName(name);
    }

    @Override
    public List<Track> findAllTracksFromPlaylist(Integer id) {
        List<Track> trackList = new ArrayList<>(trackRepository.findTracksByPlaylistId(id));
        return trackList;
    }

    @Override
    public List<Track> findAllTracksFromVideo(Integer id) {
        List<Track> trackList = new ArrayList<>(trackRepository.findTracksByVideoId(id));
        return trackList;
    }


}
