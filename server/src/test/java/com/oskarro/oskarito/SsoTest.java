package com.oskarro.oskarito;

import com.oskarro.oskarito.config.DataSeeder;
import com.oskarro.oskarito.security.sso.SsoAccountService;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Google/Facebook sign-in, with dummy client ids so the OAuth2 machinery is switched on. */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.facebook.client-id=test-fb-id",
        "spring.security.oauth2.client.registration.facebook.client-secret=test-fb-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SsoTest {

    @MockitoBean
    DataSeeder dataSeeder;

    @Autowired MockMvc mvc;
    @Autowired SsoAccountService sso;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

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
    void providersAreListedAndLoginRedirectsToGoogle() throws Exception {
        String providers = mvc.perform(get("/api/auth/providers")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(providers).contains("google").contains("facebook");
        String location = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://accounts.google.com/").contains("client_id=test-google-id");
    }

    @Test
    void newGoogleUserGetsAnActiveAccountWithAFreeUsername() {
        users.save(User.builder().username("Jan.Kowalski").email("someone-else@example.com").password("secret-pass")
                .roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow()))).build());
        User created = sso.accountFor("google", Map.of("email", "Jan@Example.com", "email_verified", true, "name", "Jan Kowalski"));
        assertThat(created.getUsername()).isEqualTo("Jan.Kowalski2");
        assertThat(created.getEmail()).isEqualTo("jan@example.com");
        assertThat(created.getActivated()).isTrue();
        assertThat(created.getAuthProvider()).isEqualTo("google");
        assertThat(created.getTermsAcceptedAt()).isNotNull();
    }

    @Test
    void existingAccountIsReusedAndActivated() {
        User existing = users.save(User.builder().username("istnieje").email("istnieje@example.com").password("secret-pass").activated(false)
                .roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow()))).build());
        User signedIn = sso.accountFor("facebook", Map.of("email", "istnieje@example.com", "name", "Ktoś"));
        assertThat(signedIn.getId()).isEqualTo(existing.getId());
        assertThat(users.findById(existing.getId()).orElseThrow().getActivated()).isTrue();
    }

    @Test
    void bannedUnverifiedOrEmaillessSignInsAreRefused() {
        users.save(User.builder().username("zbanowany").email("ban@example.com").password("secret-pass").banned(true)
                .roles(new HashSet<>(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow()))).build());
        assertThatThrownBy(() -> sso.accountFor("google", Map.of("email", "ban@example.com", "email_verified", true)))
                .hasMessage("zablokowane");
        assertThatThrownBy(() -> sso.accountFor("google", Map.of("email", "x@example.com", "email_verified", false)))
                .hasMessage("niepotwierdzony-email");
        assertThatThrownBy(() -> sso.accountFor("facebook", Map.of("name", "Bez Maila")))
                .hasMessage("brak-email");
    }
}
