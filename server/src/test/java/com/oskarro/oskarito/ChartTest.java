package com.oskarro.oskarito;

import com.oskarro.oskarito.config.DataSeeder;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackLike;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackView;
import com.oskarro.oskarito.track.TrackViewRepository;
import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChartTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @MockitoBean
    DataSeeder dataSeeder;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired TrackRepository tracks;
    @Autowired TrackLikeRepository likes;
    @Autowired TrackViewRepository views;

    @BeforeEach
    void roles() {
        for (RoleName name : RoleName.values()) {
            if (roles.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roles.save(role);
            }
        }
    }

    @Test
    void chartsArePerGenreAndPeriodAndNumbered() throws Exception {
        Instant old = Instant.now().minus(Duration.ofDays(20));
        Track fresh = track("RETRO", null, "ChartFresh");     // 2 votes this week
        Track played = track("RETRO", null, "ChartPlayed");   // 3 plays this week
        Track oldHit = track("RETRO", null, "ChartOld");      // 3 votes 20 days ago
        Track otherGenre = track("TECHNO", null, "ChartTechno");
        Track set = track("RETRO", "set", "ChartSet");
        vote(fresh, 1, Instant.now());
        vote(fresh, 1, Instant.now());
        for (int i = 0; i < 3; i++) {
            play(played, Instant.now());
            vote(oldHit, 1, old);
        }
        vote(otherGenre, 1, Instant.now());
        vote(set, 1, Instant.now());

        List<String> week = artists(chart("week", "retro"));
        assertThat(week).containsSubsequence("ChartFresh", "ChartPlayed")
                .doesNotContain("ChartOld", "ChartTechno", "ChartSet");

        List<String> month = artists(chart("month", "retro"));
        assertThat(month).containsSubsequence("ChartOld", "ChartFresh", "ChartPlayed");

        JsonNode sets = chart("week", "sety");
        assertThat(artists(sets)).contains("ChartSet").doesNotContain("ChartFresh");

        JsonNode rows = chart("all", "retro").get("rows");
        assertThat(rows.get(0).get("place").asInt()).isEqualTo(1);
        for (JsonNode row : rows) {
            assertThat(row.get("points").asInt()).isEqualTo(5 * row.get("votes").asInt() + row.get("plays").asInt());
        }

        for (int i = 0; i < 12; i++) {
            Track tie = track("HOUSE", null, "ChartTie" + i);
            play(tie, Instant.now());
        }
        var house = chart("week", "house").get("rows");
        assertThat(house).hasSize(10);
        // Even when all twelve are tied, places run 1..10 with no repeats.
        for (int i = 0; i < house.size(); i++) {
            assertThat(house.get(i).get("place").asInt()).isEqualTo(i + 1);
        }

        mvc.perform(get("/api/charts").param("period", "year").param("category", "retro")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/charts").param("period", "week").param("category", "nope")).andExpect(status().isBadRequest());
    }

    private JsonNode chart(String period, String category) throws Exception {
        return json.readTree(mvc.perform(get("/api/charts").param("period", period).param("category", category))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static List<String> artists(JsonNode chart) {
        List<String> out = new ArrayList<>();
        chart.get("rows").forEach(row -> out.add(row.get("track").get("artist").asText()));
        return out;
    }

    private Track track(String genre, String kind, String artist) {
        return tracks.save(Track.builder().artist(artist).title("T").version("")
                .url("https://hostuje.net/file.php?id=" + "e".repeat(32)).genre(genre).kind(kind)
                .setCategory(kind == null ? null : "USERS").points(0).createdAt(Instant.now()).build());
    }

    private void vote(Track track, int value, Instant at) {
        TrackLike like = new TrackLike();
        like.setTrack(track);
        like.setUser(user());
        like.setVote(value);
        like.setCreatedAt(at);
        likes.save(like);
    }

    private void play(Track track, Instant at) {
        TrackView view = new TrackView();
        view.setTrackId(track.getId());
        view.setPlayedAt(at);
        views.save(view);
    }

    private User user() {
        String name = "chart" + SEQ.incrementAndGet();
        return users.save(User.builder().username(name).email(name + "@example.com").password("x".repeat(60))
                .activated(true).roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow()))).build());
    }
}
