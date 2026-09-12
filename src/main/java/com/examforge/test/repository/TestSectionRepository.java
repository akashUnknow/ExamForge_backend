package com.examforge.test.repository;

import com.examforge.test.domain.TestSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestSectionRepository extends JpaRepository<TestSection, UUID> {

    List<TestSection> findByTestIdOrderByDisplayOrderAsc(UUID testId);

    boolean existsByTestId(UUID testId);

    boolean existsByTestIdAndNameIgnoreCase(UUID testId, String name);
}
