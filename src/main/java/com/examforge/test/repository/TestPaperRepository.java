package com.examforge.test.repository;

import com.examforge.test.domain.TestPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TestPaperRepository extends JpaRepository<TestPaper, UUID>, JpaSpecificationExecutor<TestPaper> {

    boolean existsByExamId(UUID examId);
}
