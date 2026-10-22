package com.oskarro.oskarito.track;

/** One of the current user's comments, with the track it was left under. */
public record MyCommentView(Integer id, String text, String createdAt, Integer trackId, String trackTitle, String trackArtist) {
}
