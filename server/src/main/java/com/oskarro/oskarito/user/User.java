package com.oskarro.oskarito.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min=3, max = 32)
    private String username;

    @NaturalId
    @NotNull
    @Size(max = 50)
    @Email
    private String email;

    @NotNull
    @Size(min=6, max = 100)
    @Getter(AccessLevel.NONE)
    @JsonIgnore
    private String password;

    /** Null keeps accounts created before activation able to sign in. */
    private Boolean activated;

    /**
     * False when nobody has proven the address yet (signed up while e-mail activation was off);
     * true after the mail link or a Google/Facebook sign-in; null for accounts older than this flag.
     */
    private Boolean emailVerified;

    /** Downloads left before the next activity; see DownloadAllowanceService. Null counts as 0. */
    private Integer downloadCredits;

    /** Registration time; null for accounts created before 2026-10-17. */
    private Instant createdAt;

    /** Last successful sign-in. */
    private Instant lastLoginAt;

    /** Last authenticated request (updated at most every few minutes). */
    private Instant lastSeenAt;

    /** Blocked by an administrator: cannot sign in and existing tokens stop working. */
    private Boolean banned;

    @Column(length = 200)
    private String banReason;

    /** When the user accepted the terms and privacy policy (null for accounts from before 2026-10-17). */
    private Instant termsAcceptedAt;

    /** How the account was created: null/"local" = password, "google", "facebook". */
    @Column(length = 20)
    private String authProvider;

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Column(length = 280)
    private String bio;

    @Column(length = 40)
    private String firstName;

    @Column(length = 40)
    private String lastName;

    @Column(length = 120)
    private String website;

    @Column(length = 160)
    private String favoriteGenres;

    @Column(length = 32)
    private String rankOverride;

    private Boolean autoplayNext;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

}
