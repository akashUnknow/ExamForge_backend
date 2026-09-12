package com.examforge.testseries.repository;

import com.examforge.testseries.domain.TestSeriesTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestSeriesTestRepository extends JpaRepository<TestSeriesTest, UUID> {

    List<TestSeriesTest> findByTestSeriesIdOrderByDisplayOrderAsc(UUID testSeriesId);

    boolean existsByTestSeriesIdAndTestId(UUID testSeriesId, UUID testId);

    long countByTestSeriesId(UUID testSeriesId);
}
