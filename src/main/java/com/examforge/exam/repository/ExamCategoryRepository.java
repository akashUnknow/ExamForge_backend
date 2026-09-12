package com.examforge.exam.repository;

import com.examforge.exam.domain.ExamCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamCategoryRepository extends JpaRepository<ExamCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
