package com.oskarro.oskarito.track;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
@CrossOrigin(origins = "http://localhost:4200")
public interface TrackRepository extends CrudRepository<Track, Integer> {

    List<Track> findAll();

    Optional<Track> findById(Integer id);

    Track save(Track track);

    List<Track> findTracksByProviderId(Integer id);

    List<Track> findTracksByGenre(String genre);

    List<Track> findTracksByProviderName(String name);

    List<Track> findTracksByProviderIdAndGenre(Integer id, String genre);

    List<Track> findTracksByPlaylistId(Integer id);

    List<Track> findTracksByVideoId(Integer id);

    void deleteById(Integer id);
}
