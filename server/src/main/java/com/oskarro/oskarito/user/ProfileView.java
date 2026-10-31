package com.oskarro.oskarito.user;

import java.util.List;

public record ProfileView(
        String username,
        String email,
        String bio,
        String firstName,
        String lastName,
        String website,
        String favoriteGenres,
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
        int downloadCredits,
        boolean canGrant,
        boolean autoplayNext,
        List<ProfileTrack> shared,
        /** Only for the owner: "password", "google" or "facebook". */
        String signInMethod,
        /** Only for the owner: mails about comments and likes on own tracks. */
        Boolean notifyActivity,
        /** Only for the owner: the opt-in weekly digest. */
        Boolean digestOptIn
) {
}
