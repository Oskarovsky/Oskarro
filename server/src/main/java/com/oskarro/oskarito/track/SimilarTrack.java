package com.oskarro.oskarito.track;

/** An existing track that looks like the one being added; {@code sameFile} means the very same file link. */
public record SimilarTrack(Track track, boolean sameFile, int match) {
}
