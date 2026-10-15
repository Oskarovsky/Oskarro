package com.oskarro.oskarito.playlist;

import java.util.List;
import java.util.Optional;

public interface PlaylistService {

    List<com.oskarro.oskarito.playlist.Playlist> getAllPlaylist();

    void addPlaylist(com.oskarro.oskarito.playlist.Playlist playlist);

    Optional<com.oskarro.oskarito.playlist.Playlist> findPlaylistById(Integer id);

    Optional<com.oskarro.oskarito.playlist.Playlist> findPlaylistByName(String name);

    com.oskarro.oskarito.playlist.Playlist updatePlaylist(com.oskarro.oskarito.playlist.Playlist playlist, Integer id);

    void deletePlaylistById(Integer id);

    List<com.oskarro.oskarito.playlist.Playlist> findAllPlaylistByUsername(String username);

}
