package com.oskarro.oskarito.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "site_visit", uniqueConstraints = @UniqueConstraint(name = "site_visit_visitor_day", columnNames = {"visitor", "day"}))
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String visitor;

    @Column(nullable = false)
    private LocalDate day;

    public SiteVisit(String visitor, LocalDate day) {
        this.visitor = visitor;
        this.day = day;
    }
}
