package com.oskarro.oskarito.user;

import java.util.List;

public record ProfileView(
        String username,
        String email,
        String bio,
        String rank,
        String activityRank,
        boolean granted,
        int score,
        int tracks,
        int likes,
        int dislikes,
        int commentsOnTracks,
        int commentsWritten,
        int activity,
        int downloadSlots,
        boolean canGrant,
        List<ProfileTrack> shared
) {
}
