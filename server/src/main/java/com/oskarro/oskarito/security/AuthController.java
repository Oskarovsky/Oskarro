package com.oskarro.oskarito.security;

import com.oskarro.oskarito.security.jwt.JwtTokenProvider;
import com.oskarro.oskarito.security.jwt.JwtResponse;
import com.oskarro.oskarito.user.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    PasswordResetService passwordResetService;

    @Autowired
    AccountActivationService accountActivationService;

    @Autowired
    PresenceService presenceService;

    @Autowired
    ObjectProvider<ClientRegistrationRepository> ssoProviders;

    @PostMapping(value = "/signin")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (LockedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseMessage("Konto zostało zablokowane przez administratora."));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseMessage("Konto nie jest jeszcze aktywne. Otwórz link z maila aktywacyjnego."));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateJwtToken(authentication);
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        presenceService.loggedIn(userDetails.getId());
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    /** Which external sign-ins are switched on, e.g. ["google","facebook"]; empty until keys are configured. */
    @GetMapping("/providers")
    public List<String> providers() {
        ClientRegistrationRepository repository = ssoProviders.getIfAvailable();
        List<String> ids = new java.util.ArrayList<>();
        if (repository instanceof Iterable<?> registrations) {
            for (Object registration : registrations) {
                ids.add(((ClientRegistration) registration).getRegistrationId());
            }
        }
        return ids;
    }

    /** Session details for a token obtained through Google/Facebook (same shape as /signin). */
    @GetMapping("/session")
    public ResponseEntity<?> session(@RequestHeader(value = "Authorization", required = false) String header) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (header == null || !header.startsWith("Bearer ") || authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ResponseMessage("Zaloguj się ponownie"));
        }
        List<String> roles = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(header.substring(7), principal.getId(), principal.getUsername(), principal.getEmail(), roles));
    }

    @PostMapping("/signup")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signupRequest) {
        if (filled(signupRequest.getCompany()) || filled(signupRequest.getHp())) {
            return ResponseEntity.badRequest().body(new ResponseMessage("Nie udało się założyć konta."));
        }

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage("Ta nazwa użytkownika jest już zajęta."));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage("Ten adres email jest już używany."));
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(encoder.encode(signupRequest.getPassword()))
                .activated(false)
                .createdAt(java.time.Instant.now())
                .termsAcceptedAt(java.time.Instant.now())
                .build();

        // Role is never taken from the request: self-registered accounts are always plain users.
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
        Set<Role> roles = new HashSet<>(Set.of(userRole));
        user.setRoles(roles);
        userRepository.save(user);
        boolean delivered = accountActivationService.issueAndSend(user);

        return ResponseEntity.ok(java.util.Map.of(
                "message", delivered
                        ? "Konto utworzone. Otwórz link z maila, żeby je aktywować."
                        : "Konto zapisane, ale mail aktywacyjny nie wyszedł.",
                "delivered", delivered,
                "activationRequired", true));
    }

    @PostMapping("/activate")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> activate(@RequestBody java.util.Map<String, String> body) {
        String token = body == null ? "" : body.get("token");
        accountActivationService.activate(token);
        return ResponseEntity.ok(new ResponseMessage("Konto jest aktywne. Możesz się zalogować."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalidSignup(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("Sprawdź dane formularza.");
        return ResponseEntity.badRequest().body(new ResponseMessage(message));
    }

    private boolean filled(String value) {
        return value != null && !value.isBlank();
    }

    @PostMapping("/forgot")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> forgot(@RequestBody java.util.Map<String, String> body) {
        String email = body == null ? "" : body.get("email");
        boolean delivered = passwordResetService.request(email);
        return ResponseEntity.ok(java.util.Map.of("ok", true, "delivered", delivered));
    }

    @PostMapping("/reset")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> reset(@RequestBody java.util.Map<String, String> body) {
        java.util.Map<String, String> note = body == null ? java.util.Map.of() : body;
        passwordResetService.reset(note.get("token"), note.get("password"));
        return ResponseEntity.ok(new ResponseMessage("Password updated"));
    }
}
