package com.oskarro.oskarito.report;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A report sent through the "Zgłoś naruszenie" form (copyright, spam, broken file…). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "content_report", indexes = @Index(columnList = "status"))
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** COPYRIGHT, SPAM, OFFENSIVE, BROKEN_LINK, OTHER. */
    @Column(nullable = false, length = 20)
    private String reason;

    private Integer trackId;

    @Column(length = 300)
    private String url;

    @Column(nullable = false, length = 2000)
    private String details;

    @Column(length = 100)
    private String reporterName;

    @Column(length = 120)
    private String reporterEmail;

    /** Username when the reporter was signed in. */
    @Column(length = 32)
    private String reporterUser;

    /** OPEN or RESOLVED. */
    @Column(nullable = false, length = 10)
    private String status;

    @Column(length = 500)
    private String resolution;

    @Column(length = 32)
    private String resolvedBy;

    private Instant resolvedAt;

    @Column(nullable = false)
    private Instant createdAt;
}
