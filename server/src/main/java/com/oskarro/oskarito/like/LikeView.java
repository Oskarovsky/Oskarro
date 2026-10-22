package com.oskarro.oskarito.like;

/** How many people like the item, whether the current user does, and their download credits. */
public record LikeView(long count, boolean liked, int downloadCredits) {
}
