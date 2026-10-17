package com.oskarro.oskarito.stats;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface SiteVisitRepository extends CrudRepository<SiteVisit, Long> {

    boolean existsByVisitorAndDay(String visitor, LocalDate day);

    long countByDay(LocalDate day);

    @Query("select v.day, count(v) from SiteVisit v where v.day >= :from group by v.day")
    List<Object[]> visitorsPerDaySince(@Param("from") LocalDate from);
}
