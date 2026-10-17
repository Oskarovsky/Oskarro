package com.oskarro.oskarito.security.sso;

import com.oskarro.oskarito.security.jwt.JwtTokenProvider;
import com.oskarro.oskarito.user.PresenceService;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After Google/Facebook confirms the user, issue our own JWT and hand it to the Angular app in the URL
 * fragment (fragments never reach servers or logs). The OAuth session is discarded right away.
 */
@Component
public class SsoHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SsoHandlers.class);
    private static final String DONE = "/logowanie/sso";

    private final SsoAccountService accounts;
    private final JwtTokenProvider tokens;
    private final PresenceService presence;

    public SsoHandlers(SsoAccountService accounts, JwtTokenProvider tokens, PresenceService presence) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.presence = presence;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
        String provider = oauth.getAuthorizedClientRegistrationId();
        try {
            User user = accounts.accountFor(provider, oauth.getPrincipal().getAttributes());
            UserPrincipal principal = UserPrincipal.build(user);
            String jwt = tokens.generateJwtToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            presence.loggedIn(user.getId());
            finish(request);
            response.sendRedirect(DONE + "#token=" + jwt);
        } catch (SsoAccountService.SsoRefused refused) {
            finish(request);
            response.sendRedirect(DONE + "#blad=" + refused.code());
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("SSO sign-in failed: {}", exception.getClass().getSimpleName());
        finish(request);
        response.sendRedirect(DONE + "#blad=anulowane");
    }

    private static void finish(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
