package com.examforge.testseries.repository;

import com.examforge.testseries.domain.TestSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TestSeriesRepository extends JpaRepository<TestSeries, UUID>, JpaSpecificationExecutor<TestSeries> {

    boolean existsByExamId(UUID examId);
}
