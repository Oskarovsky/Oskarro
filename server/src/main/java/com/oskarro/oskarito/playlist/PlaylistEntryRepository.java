package com.oskarro.oskarito.playlist;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PlaylistEntryRepository extends CrudRepository<PlaylistEntry, Integer> {

    List<PlaylistEntry> findByPlaylist_IdOrderByIdAsc(Integer playlistId);

    boolean existsByPlaylist_IdAndTrack_Id(Integer playlistId, Integer trackId);

    long countByPlaylist_Id(Integer playlistId);

    void deleteByPlaylist_Id(Integer playlistId);

    void deleteByPlaylist_IdAndTrack_Id(Integer playlistId, Integer trackId);

    void deleteByTrack_Id(Integer trackId);
}
