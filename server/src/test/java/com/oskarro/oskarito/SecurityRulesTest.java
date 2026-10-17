package com.oskarro.oskarito;

import tools.jackson.databind.ObjectMapper;
import com.oskarro.oskarito.config.DataSeeder;
import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.playlist.PlaylistEntry;
import com.oskarro.oskarito.playlist.PlaylistEntryRepository;
import com.oskarro.oskarito.playlist.PlaylistRepository;
import com.oskarro.oskarito.storage.ImageRepository;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackComment;
import com.oskarro.oskarito.track.TrackCommentRepository;
import com.oskarro.oskarito.track.TrackLike;
import com.oskarro.oskarito.track.TrackLikeRepository;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.track.TrackViewRepository;
import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import com.oskarro.oskarito.video.Video;
import com.oskarro.oskarito.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regression tests for the access-control holes found in the October 2026 review. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

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
    @Autowired TrackLikeRepository likes;
    @Autowired TrackCommentRepository comments;
    @Autowired PlaylistRepository playlists;
    @Autowired PlaylistEntryRepository entries;
    @Autowired VideoRepository videos;
    @Autowired ImageRepository images;
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
    void signupIgnoresRequestedAdminRole() throws Exception {
        String name = "intruder" + SEQ.incrementAndGet();
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", name, "email", name + "@example.com",
                                "password", PASSWORD, "role", List.of("admin", "mod"), "acceptTerms", true))))
                .andExpect(status().isOk());
        User user = users.findByUsername(name).orElseThrow();
        user.setActivated(true);
        users.save(user);

        String body = mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", name, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("roles").toString()).isEqualTo("[\"ROLE_USER\"]");
    }

    @Test
    void trackDeletionNeedsAuthorOrStaff() throws Exception {
        User author = user(RoleName.ROLE_USER);
        User stranger = user(RoleName.ROLE_USER);
        Track track = track(author);
        TrackLike like = new TrackLike();
        like.setTrack(track);
        like.setUser(stranger);
        like.setVote(1);
        likes.save(like);
        TrackComment comment = new TrackComment();
        comment.setTrack(track);
        comment.setAuthor(stranger);
        comment.setText("nice");
        comments.save(comment);

        mvc.perform(delete("/api/tracks/" + track.getId())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/tracks/" + track.getId()).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        assertThat(tracks.findById(track.getId())).isPresent();

        mvc.perform(delete("/api/tracks/" + track.getId()).header("Authorization", bearer(author)))
                .andExpect(status().isOk());
        assertThat(tracks.findById(track.getId())).isEmpty();
        assertThat(likes.findByTrackIdAndUserId(track.getId(), stranger.getId())).isEmpty();
    }

    @Test
    void moderatorCanDeleteAnyTrack() throws Exception {
        Track track = track(user(RoleName.ROLE_USER));
        mvc.perform(delete("/api/tracks/" + track.getId()).header("Authorization", bearer(user(RoleName.ROLE_MODERATOR))))
                .andExpect(status().isOk());
        assertThat(tracks.findById(track.getId())).isEmpty();
    }

    @Test
    void videosAreStaffOnly() throws Exception {
        Video video = videos.save(Video.builder().name("Mix").url("abc").category("MIX").build());
        String draft = json.writeValueAsString(Map.of("name", "Spam", "url", "x", "category", "MIX"));

        mvc.perform(post("/api/video/add").contentType(MediaType.APPLICATION_JSON).content(draft))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/video/add").contentType(MediaType.APPLICATION_JSON).content(draft)
                        .header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/video/" + video.getId()).header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        assertThat(videos.findById(video.getId())).isPresent();

        mvc.perform(delete("/api/video/" + video.getId()).header("Authorization", bearer(user(RoleName.ROLE_ADMIN))))
                .andExpect(status().isOk());
        assertThat(videos.findById(video.getId())).isEmpty();
    }

    @Test
    void rawTrackAddCannotOverwriteExistingTrack() throws Exception {
        User author = user(RoleName.ROLE_USER);
        Track existing = track(author);
        String forged = json.writeValueAsString(Map.of(
                "id", existing.getId(), "artist", "Hacked", "title", "Hacked",
                "url", "https://hostuje.net/file.php?id=" + "b".repeat(32), "points", 9999, "genre", "CLUB"));

        mvc.perform(post("/api/tracks/add").contentType(MediaType.APPLICATION_JSON).content(forged)
                        .header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tracks/add").contentType(MediaType.APPLICATION_JSON).content(forged)
                        .header("Authorization", bearer(user(RoleName.ROLE_ADMIN))))
                .andExpect(status().isOk());

        Track reloaded = tracks.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Original");
        assertThat(reloaded.getAuthor().getId()).isEqualTo(author.getId());
    }

    @Test
    void avatarUploadIsForSelfAndNeverTouchesDisk() throws Exception {
        Path sentinel = Path.of("uploads", "covers", "sentinel.jpg");
        Files.createDirectories(sentinel.getParent());
        Files.write(sentinel, new byte[]{1});
        MockMultipartFile file = new MockMultipartFile("file", "../../evil.png", "image/png", png());

        mvc.perform(multipart("/api/storage/upload").file(file).param("username", "covers"))
                .andExpect(status().isUnauthorized());

        User me = user(RoleName.ROLE_USER);
        User victim = user(RoleName.ROLE_USER);
        mvc.perform(multipart("/api/storage/upload").file(file).param("username", victim.getUsername())
                        .header("Authorization", bearer(me)))
                .andExpect(status().isOk());

        assertThat(images.existsByUserId(me.getId())).isTrue();
        assertThat(images.existsByUserId(victim.getId())).isFalse();
        assertThat(sentinel).exists();
        assertThat(Path.of("evil.png")).doesNotExist();
        Files.deleteIfExists(sentinel);
    }

    @Test
    void unpinRemovesOnlyTheEntryAndOnlyForTheOwner() throws Exception {
        User owner = user(RoleName.ROLE_USER);
        Track someoneElses = track(user(RoleName.ROLE_USER));
        Playlist playlist = playlists.save(Playlist.builder().name("Mine").user(owner).build());
        PlaylistEntry entry = new PlaylistEntry();
        entry.setPlaylist(playlist);
        entry.setTrack(someoneElses);
        entries.save(entry);

        String path = "/api/playlist/" + playlist.getId() + "/pin/" + someoneElses.getId();
        mvc.perform(delete(path).header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization", bearer(owner))).andExpect(status().isOk());

        assertThat(entries.existsByPlaylist_IdAndTrack_Id(playlist.getId(), someoneElses.getId())).isFalse();
        assertThat(tracks.findById(someoneElses.getId())).isPresent();
    }

    @Test
    void votingIsOnePointPerUserAndCanBeUndone() throws Exception {
        Track track = track(user(RoleName.ROLE_USER));
        String voter = bearer(user(RoleName.ROLE_USER));
        String up = "{\"vote\":1}";
        String down = "{\"vote\":-1}";
        String path = "/api/tracks/" + track.getId() + "/vote";

        String first = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(up).header("Authorization", voter))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(first).get("score").asInt()).isEqualTo(1);

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(down).header("Authorization", voter))
                .andExpect(status().isConflict());

        String undone = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(up).header("Authorization", voter))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(undone).get("score").asInt()).isEqualTo(0);

        String other = bearer(user(RoleName.ROLE_USER));
        String total = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(down).header("Authorization", other))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(total).get("score").asInt()).isEqualTo(-1);
    }

    @Test
    void repeatedPlaysFromOneListenerCountOnce() throws Exception {
        Track track = track(user(RoleName.ROLE_USER));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/tracks/" + track.getId() + "/view").header("X-Real-IP", "10.0.0.1")).andExpect(status().isOk());
        }
        mvc.perform(post("/api/tracks/" + track.getId() + "/view").header("X-Real-IP", "10.0.0.2")).andExpect(status().isOk());
        assertThat(views.countsAll(List.of(track.getId())).get(0)[1]).isEqualTo(2L);
    }

    @Test
    void growthStatsCoverEveryDayOfTheRange() throws Exception {
        track(user(RoleName.ROLE_USER));
        String body = mvc.perform(get("/api/stats/growth").param("days", "14"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var tree = json.readTree(body);
        assertThat(tree.get("days").size()).isEqualTo(14);
        assertThat(tree.get("days").get(13).get("tracks").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(tree.get("tracks").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void adminPanelIsAdminOnly() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(user(RoleName.ROLE_MODERATOR))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization", bearer(user(RoleName.ROLE_ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void banCutsOffExistingSessionsAndLogins() throws Exception {
        User target = user(RoleName.ROLE_USER);
        String targetToken = bearer(target);
        User adminUser = user(RoleName.ROLE_ADMIN);
        String admin = bearer(adminUser);

        mvc.perform(patch("/api/admin/users/" + target.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"banned\":true,\"banReason\":\"spam\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/profile/me").header("Authorization", targetToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", target.getUsername(), "password", PASSWORD))))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/admin/users/" + adminUser.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"banned\":true}"))
                .andExpect(status().isBadRequest());

        String log = mvc.perform(get("/api/admin/audit").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(log).contains(target.getUsername()).contains("spam");
    }

    @Test
    void adminCanEditAnyTrackAndComment() throws Exception {
        Track track = track(user(RoleName.ROLE_USER));
        String admin = bearer(user(RoleName.ROLE_ADMIN));
        String change = json.writeValueAsString(Map.of("artist", "Fixed", "title", "Better", "version", "",
                "genre", "dance", "url", "https://soundfiles.eu/AbCdEf123456"));
        mvc.perform(put("/api/admin/tracks/" + track.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(change))
                .andExpect(status().isOk());
        Track saved = tracks.findById(track.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Better");
        assertThat(saved.getGenre()).isEqualTo("DANCE");
        assertThat(saved.getUrl()).isEqualTo("https://soundfiles.eu/AbCdEf123456/file");

        String users = mvc.perform(get("/api/admin/users").param("filter", "staff").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(users).get("total").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void setsLiveInTheirOwnSectionAndNewGenresWork() throws Exception {
        String token = bearer(user(RoleName.ROLE_USER));
        String set = json.writeValueAsString(Map.of("artist", "DJ Test", "title", "Live at Club X", "genre", "hands-up",
                "url", "https://soundfiles.eu/SetSet123456", "kind", "set", "setCategory", "kluby",
                "eventName", "Club X", "tracklist", "1. Intro\n2. Outro"));
        String saved = mvc.perform(post("/api/tracks/share").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(set))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var savedTree = json.readTree(saved);
        assertThat(savedTree.get("kind").asText()).isEqualTo("set");
        assertThat(savedTree.get("setCategory").asText()).isEqualTo("CLUBS");
        assertThat(savedTree.get("genre").asText()).isEqualTo("HANDS_UP");
        int id = savedTree.get("id").asInt();

        assertThat(mvc.perform(get("/api/tracks/sets").param("category", "kluby"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).contains("\"id\":" + id);
        assertThat(mvc.perform(get("/api/tracks/sets").param("category", "festiwale"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).doesNotContain("\"id\":" + id);
        assertThat(mvc.perform(get("/api/tracks/genre/hands-up"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).doesNotContain("\"id\":" + id);
        mvc.perform(get("/api/tracks/sets").param("category", "disco")).andExpect(status().isBadRequest());

        String pop = json.writeValueAsString(Map.of("artist", "A", "title", "B", "genre", "pop",
                "url", "https://soundfiles.eu/PopPop123456"));
        mvc.perform(post("/api/tracks/share").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(pop))
                .andExpect(status().isOk());
        assertThat(mvc.perform(get("/api/tracks/genre/pop")).andReturn().getResponse().getContentAsString()).contains("PopPop123456");
    }

    @Test
    void prometheusMetricsAreExposedForTheLocalScraper() throws Exception {
        String body = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("jvm_memory_used_bytes").contains("application=\"oskarro\"");
    }

    @Test
    void signupRequiresAcceptingTheTerms() throws Exception {
        String name = "noterms" + SEQ.incrementAndGet();
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", name, "email", name + "@example.com", "password", PASSWORD))))
                .andExpect(status().isBadRequest());
        assertThat(users.findByUsername(name)).isEmpty();
    }

    @Test
    void usersCanExportAndDeleteTheirAccount() throws Exception {
        User me = user(RoleName.ROLE_USER);
        String token = bearer(me);
        Track mine = track(me);
        Track other = track(user(RoleName.ROLE_USER));
        mvc.perform(post("/api/tracks/" + other.getId() + "/vote").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vote\":1}")).andExpect(status().isOk());
        mvc.perform(post("/api/tracks/" + other.getId() + "/comments").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"super\"}")).andExpect(status().isOk());

        String export = mvc.perform(get("/api/profile/me/export").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var tree = json.readTree(export);
        assertThat(tree.get("account").get("email").asText()).isEqualTo(me.getEmail());
        assertThat(tree.get("tracks").size()).isEqualTo(1);
        assertThat(tree.get("comments").size()).isEqualTo(1);
        assertThat(tree.get("votes").size()).isEqualTo(1);

        mvc.perform(delete("/api/profile/me").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":\"wrong\"}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/profile/me").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("confirm", PASSWORD))))
                .andExpect(status().isOk());

        assertThat(users.findByUsername(me.getUsername())).isEmpty();
        assertThat(tracks.findById(mine.getId())).isEmpty();
        assertThat(tracks.findById(other.getId())).isPresent();
        assertThat(likes.findByTrackIdAndUserId(other.getId(), me.getId())).isEmpty();
        mvc.perform(get("/api/profile/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void reportsReachTheAdminQueue() throws Exception {
        Track track = track(user(RoleName.ROLE_USER));
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("reason", "COPYRIGHT", "trackId", track.getId(), "details", "To mój utwór, nie zgadzam się"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("reason", "COPYRIGHT", "trackId", track.getId(), "details", "To mój utwór, nie zgadzam się",
                                "email", "label@example.com", "name", "Label"))))
                .andExpect(status().isOk());

        String admin = bearer(user(RoleName.ROLE_ADMIN));
        mvc.perform(get("/api/admin/reports").header("Authorization", bearer(user(RoleName.ROLE_USER))))
                .andExpect(status().isForbidden());
        String open = mvc.perform(get("/api/admin/reports").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var report = json.readTree(open).get(0);
        assertThat(report.get("reporterEmail").asText()).isEqualTo("label@example.com");
        assertThat(report.get("trackTitle").asText()).contains("Original");

        mvc.perform(patch("/api/admin/reports/" + report.get("id").asLong()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"resolution\":\"usunięto\"}"))
                .andExpect(status().isOk());
        assertThat(mvc.perform(get("/api/admin/reports").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString()).doesNotContain("label@example.com");
    }

    @Test
    void ssoIsOffWithoutKeys() throws Exception {
        assertThat(mvc.perform(get("/api/auth/providers")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).isEqualTo("[]");
    }

    private User user(RoleName role) {
        String name = "user" + SEQ.incrementAndGet();
        return users.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password(encoder.encode(PASSWORD))
                .activated(true)
                .roles(new HashSet<>(Set.of(roles.findByName(role).orElseThrow())))
                .build());
    }

    private Track track(User author) {
        return tracks.save(Track.builder()
                .artist("Artist").title("Original").version("")
                .url("https://hostuje.net/file.php?id=" + "a".repeat(32))
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

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
