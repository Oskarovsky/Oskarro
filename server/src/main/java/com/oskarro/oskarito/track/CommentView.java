package com.oskarro.oskarito.track;

/** {@code avatar} tells the page whether /api/storage/{author}/avatar has a picture, so it can skip the request. */
public record CommentView(Integer id, String text, String author, String createdAt, boolean avatar) {
}
