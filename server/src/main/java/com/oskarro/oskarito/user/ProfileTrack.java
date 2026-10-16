package com.oskarro.oskarito.user;

public record ProfileTrack(Integer id, String title, String artist, String version, String genre, int score, boolean hasCover) {
}
