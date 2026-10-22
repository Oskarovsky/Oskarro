package com.oskarro.oskarito;

import tools.jackson.databind.ObjectMapper;
import com.oskarro.oskarito.config.DataSeeder;
import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.playlist.PlaylistRepository;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Download credits: like 3, comment 5, new track 7; the balance is the larger value, never a sum. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DownloadCreditsTest {

    private static final String PASSWORD = "secret-pass";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @MockitoBean
    DataSeeder dataSeeder;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired TrackRepository tracks;
    @Autowired PlaylistRepository playlists;

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
    void creditsTakeTheLargerValueAndAreSpentOncePerTrack() throws Exception {
        User author = user();
        User fan = user();
        String auth = bearer(fan);
        Track first = track(author);
        Track second = track(author);

        assertThat(credits(fan)).isZero();
        mvc.perform(post("/api/tracks/" + first.getId() + "/download").header("Authorization", auth))
                .andExpect(status().isForbidden());

        vote(auth, first, 1);
        assertThat(credits(fan)).isEqualTo(3);

        comment(auth, first);
        assertThat(credits(fan)).isEqualTo(5);

        // A like after a comment does not lower the balance, and nothing adds up.
        vote(auth, second, 1);
        assertThat(credits(fan)).isEqualTo(5);

        String body = mvc.perform(post("/api/tracks/" + first.getId() + "/download").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("url").asText()).startsWith("https://hostuje.net/");
        assertThat(credits(fan)).isEqualTo(4);

        // Downloading the same track again is free.
        mvc.perform(post("/api/tracks/" + first.getId() + "/download").header("Authorization", auth))
                .andExpect(status().isOk());
        assertThat(credits(fan)).isEqualTo(4);

        String page = mvc.perform(get("/api/tracks/id/" + first.getId()).header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(page).get("downloadUnlocked").asBoolean()).isTrue();
        assertThat(page).doesNotContain("downloadUrl");
    }

    @Test
    void repeatingTheSameActivityGivesNothingNew() throws Exception {
        User author = user();
        User fan = user();
        String auth = bearer(fan);
        Track track = track(author);
        Track other = track(author);

        vote(auth, track, 1);
        mvc.perform(post("/api/tracks/" + other.getId() + "/download").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(post("/api/tracks/" + track.getId() + "/download").header("Authorization", auth)).andExpect(status().isOk());
        assertThat(credits(fan)).isEqualTo(1);

        // Taking the vote back and voting again must not refill the balance.
        vote(auth, track, 1);
        vote(auth, track, 1);
        assertThat(credits(fan)).isEqualTo(1);
    }

    @Test
    void ownContentGivesNoCreditsButANewTrackGivesSeven() throws Exception {
        User author = user();
        String auth = bearer(author);
        Track own = track(author);
        vote(auth, own, 1);
        comment(auth, own);
        assertThat(credits(author)).isZero();

        mvc.perform(post("/api/tracks/share").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("artist", "A", "title", "T", "genre", "club",
                                "url", "https://hostuje.net/file.php?id=" + "e".repeat(32)))))
                .andExpect(status().isOk());
        assertThat(credits(author)).isEqualTo(7);
    }

    @Test
    void playlistsAndVideosCanBeLikedForCredits() throws Exception {
        User owner = user();
        User fan = user();
        String auth = bearer(fan);
        Playlist playlist = playlists.save(Playlist.builder().name("Na siłownię").user(owner).build());
        Playlist secret = playlists.save(Playlist.builder().name("Tajna").user(owner).privatePlaylist(true).build());

        String body = mvc.perform(post("/api/likes/playlist/" + playlist.getId()).header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("liked").asBoolean()).isTrue();
        assertThat(json.readTree(body).get("count").asLong()).isEqualTo(1);
        assertThat(json.readTree(body).get("downloadCredits").asInt()).isEqualTo(3);

        mvc.perform(post("/api/likes/video/dQw4w9WgXcQ").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/likes/video/dQw4w9WgXcQ")).andExpect(status().isOk());

        mvc.perform(post("/api/likes/playlist/" + secret.getId()).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/likes/video/not-a-video!").header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/likes/playlist/" + playlist.getId())).andExpect(status().isUnauthorized());

        // The owner liking their own playlist gets nothing.
        mvc.perform(post("/api/likes/playlist/" + playlist.getId()).header("Authorization", bearer(owner))).andExpect(status().isOk());
        assertThat(credits(owner)).isZero();
    }

    private int credits(User user) {
        Integer value = users.findById(user.getId()).orElseThrow().getDownloadCredits();
        return value == null ? 0 : value;
    }

    private void vote(String auth, Track track, int value) throws Exception {
        mvc.perform(post("/api/tracks/" + track.getId() + "/vote").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("vote", value))))
                .andExpect(status().isOk());
    }

    private void comment(String auth, Track track) throws Exception {
        mvc.perform(post("/api/tracks/" + track.getId() + "/comments").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text", "Petarda"))))
                .andExpect(status().isOk());
    }

    private User user() {
        String name = "dl" + SEQ.incrementAndGet();
        return users.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password(encoder.encode(PASSWORD))
                .activated(true)
                .roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow())))
                .build());
    }

    private Track track(User author) {
        return tracks.save(Track.builder()
                .artist("Artist").title("Numer " + SEQ.incrementAndGet()).version("")
                .url("https://hostuje.net/file.php?id=" + Integer.toHexString(SEQ.get()).repeat(32).substring(0, 32))
                .genre("CLUB").points(0).createdAt(Instant.now()).author(author)
                .build());
    }

    private String bearer(User user) throws Exception {
        String body = mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", user.getUsername(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }
}
