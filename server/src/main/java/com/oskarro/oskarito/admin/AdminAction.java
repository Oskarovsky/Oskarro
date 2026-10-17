package com.oskarro.oskarito.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Audit trail entry: who changed what in the admin panel. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "admin_action", indexes = @Index(columnList = "createdAt"))
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String actor;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 20)
    private String targetType;

    private Integer targetId;

    @Column(length = 120)
    private String targetLabel;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;
}
