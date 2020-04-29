package com.oskarro.oskarito.track;

import java.util.List;
import java.util.Optional;

public interface TrackService {

    List<Track> findAll();

    Optional<Track> findById(Integer id);

    Track saveTrack(Track track);

    List<Track> findByProviderId(Integer id);

    List<Track> findTracksByGenre(String genre);

    List<Track> findByProviderIdAndGenre(Integer id, String genre);

    List<Track> findTracksByProviderName(String name);

    List<Track> findAllTracksFromPlaylist(Integer id);

    List<Track> findAllTracksFromVideo(Integer id);
}
