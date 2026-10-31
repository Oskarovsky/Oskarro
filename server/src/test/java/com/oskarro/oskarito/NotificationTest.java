package com.oskarro.oskarito;

import tools.jackson.databind.ObjectMapper;
import com.oskarro.oskarito.config.DataSeeder;
import com.oskarro.oskarito.notify.NotificationService;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.user.MailService;
import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mails to authors about comments and likes, the opt-in weekly digest, unsubscribe links and welcome tokens. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationTest {

    private static final String PASSWORD = "secret-pass";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @MockitoBean
    DataSeeder dataSeeder;

    @MockitoBean
    MailService mail;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired TrackRepository tracks;
    @Autowired NotificationService notifications;

    @BeforeEach
    void roles() {
        for (RoleName name : RoleName.values()) {
            if (roles.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roles.save(role);
            }
        }
        clearInvocations(mail);
    }

    @Test
    void authorHearsAboutCommentsAndCanUnsubscribeWithOneClick() throws Exception {
        User author = user();
        User fan = user();
        Track track = track(author, "CLUB");
        String auth = bearer(fan);

        comment(auth, track, "Petarda!");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(3000)).send(eq(author.getEmail()), contains("skomentował"), body.capture());
        assertThat(body.getValue()).contains("Petarda!").contains("/track/" + track.getId());

        Matcher link = Pattern.compile("/api/notify/unsubscribe\\?u=(\\d+)&k=activity&t=([0-9a-f]+)").matcher(body.getValue());
        assertThat(link.find()).isTrue();
        mvc.perform(get("/api/notify/unsubscribe").param("u", link.group(1)).param("k", "activity").param("t", "deadbeef"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/notify/unsubscribe").param("u", link.group(1)).param("k", "activity").param("t", link.group(2)))
                .andExpect(status().isOk());
        assertThat(users.findById(author.getId()).orElseThrow().getNotifyActivity()).isFalse();

        clearInvocations(mail);
        comment(auth, track, "Jeszcze raz");
        verify(mail, after(500).never()).send(eq(author.getEmail()), anyString(), anyString());
    }

    @Test
    void ownCommentsSendNothingAndLikesAreThrottled() throws Exception {
        User author = user();
        Track track = track(author, "CLUB");
        comment(bearer(author), track, "Mój numer");
        verify(mail, after(500).never()).send(anyString(), anyString(), anyString());

        Track second = track(author, "CLUB");
        vote(bearer(user()), track);
        verify(mail, timeout(3000)).send(eq(author.getEmail()), contains("polubił"), anyString());
        clearInvocations(mail);
        vote(bearer(user()), second);
        verify(mail, after(500).never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void weeklyDigestGoesOnlyToOptedInUsersAndMatchesTheirGenres() {
        User author = user();
        Track vixa = track(author, "VIXA");
        track(author, "TECHNO");
        User reader = user();
        reader.setDigestOptIn(true);
        reader.setFavoriteGenres("VIXA");
        users.save(reader);
        User silent = user();

        notifications.weeklyDigest();

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(3000)).send(eq(reader.getEmail()), contains("Nowości"), body.capture());
        assertThat(body.getValue()).contains("/track/" + vixa.getId()).doesNotContain("TECHNO");
        verify(mail, after(500).never()).send(eq(silent.getEmail()), anyString(), anyString());
    }

    @Test
    void newAccountsStartWithThreeTokens() throws Exception {
        String name = "nowy" + SEQ.incrementAndGet();
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", name, "email", name + "@example.com",
                                "password", PASSWORD, "acceptTerms", true))))
                .andExpect(status().isOk());
        assertThat(users.findByUsername(name).orElseThrow().getDownloadCredits()).isEqualTo(3);
    }

    private void comment(String auth, Track track, String text) throws Exception {
        mvc.perform(post("/api/tracks/" + track.getId() + "/comments").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text", text))))
                .andExpect(status().isOk());
    }

    private void vote(String auth, Track track) throws Exception {
        mvc.perform(post("/api/tracks/" + track.getId() + "/vote").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("vote", 1))))
                .andExpect(status().isOk());
    }

    private User user() {
        String name = "nt" + SEQ.incrementAndGet();
        return users.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password(encoder.encode(PASSWORD))
                .activated(true)
                .roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow())))
                .build());
    }

    private Track track(User author, String genre) {
        int n = SEQ.incrementAndGet();
        return tracks.save(Track.builder()
                .artist("Artist").title("Numer " + genre + " " + n).version("")
                .url("https://hostuje.net/file.php?id=" + String.format("%032x", n))
                .genre(genre).points(0).createdAt(Instant.now()).author(author)
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
