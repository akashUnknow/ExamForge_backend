package com.examforge.study.repository;

import com.examforge.study.domain.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, UUID>, JpaSpecificationExecutor<StudyMaterial> {
}
