package com.oskarro.oskarito.playlist;

import java.util.List;
import java.util.Optional;

public interface PlaylistService {

    List<com.oskarro.oskarito.playlist.Playlist> getAllPlaylist();

    PlaylistView create(PlaylistDraft draft);

    PlaylistView view(Integer id);

    List<PlaylistView> listAll();

    List<PlaylistView> listByUsername(String username);

    void deleteOwned(Integer id);

    Optional<com.oskarro.oskarito.playlist.Playlist> findPlaylistById(Integer id);

    Optional<com.oskarro.oskarito.playlist.Playlist> findPlaylistByName(String name);

    com.oskarro.oskarito.playlist.Playlist updatePlaylist(com.oskarro.oskarito.playlist.Playlist playlist, Integer id);

    void deletePlaylistById(Integer id);

    List<com.oskarro.oskarito.playlist.Playlist> findAllPlaylistByUsername(String username);

}
