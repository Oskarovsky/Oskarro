package com.oskarro.oskarito.stats;

import java.util.List;

/** Daily activity for the home page charts plus all-time totals (for cumulative lines). */
public record GrowthStats(List<Day> days, long tracks, long comments, long plays, long votes, long users) {

    public record Day(String date, long tracks, long comments, long plays, long votes, long users, long visitors) {
    }
}
