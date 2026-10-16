package com.oskarro.oskarito.stats;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface SiteVisitRepository extends CrudRepository<SiteVisit, Long> {

    boolean existsByVisitorAndDay(String visitor, LocalDate day);

    long countByDay(LocalDate day);
}
