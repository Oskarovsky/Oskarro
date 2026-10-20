package com.oskarro.oskarito.playlist;

/** {@code mine} is true when the signed-in user owns the playlist. */
public record PlaylistView(Integer id, String name, String description, String owner, int tracks,
                           boolean privatePlaylist, boolean mine) {
}
