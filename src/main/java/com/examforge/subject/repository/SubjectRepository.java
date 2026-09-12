package com.examforge.subject.repository;

import com.examforge.subject.domain.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Page<Subject> findByExamIdOrderByDisplayOrderAsc(UUID examId, Pageable pageable);

    boolean existsByExamId(UUID examId);

    boolean existsByExamIdAndNameIgnoreCase(UUID examId, String name);
}
