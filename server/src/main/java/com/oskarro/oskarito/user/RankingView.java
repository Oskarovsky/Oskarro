package com.oskarro.oskarito.user;

import java.util.List;

public record RankingView(
        String formula,
        List<RankStep> ranks,
        List<BoardRow> mostTracks,
        List<BoardRow> mostActive,
        List<BoardRow> thisMonth,
        Progress me
) {
    public record RankStep(String title, int minScore, String how) {
    }

    public record BoardRow(int place, String username, String rank, int tracks, int monthTracks, int score) {
    }

    public record Progress(String username, int score, String rank, String nextRank, Integer pointsToNext) {
    }
}
