package com.oskarro.oskarito.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    List<ContentReport> findByStatusOrderByCreatedAtDesc(String status);

    List<ContentReport> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
