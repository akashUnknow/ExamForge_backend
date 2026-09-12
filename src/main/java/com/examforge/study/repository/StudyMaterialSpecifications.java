package com.examforge.study.repository;

import com.examforge.study.domain.MaterialType;
import com.examforge.study.domain.StudyMaterial;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class StudyMaterialSpecifications {

    private StudyMaterialSpecifications() {
    }

    public static Specification<StudyMaterial> hasExam(UUID examId) {
        return (root, query, cb) -> examId == null ? null : cb.equal(root.get("exam").get("id"), examId);
    }

    public static Specification<StudyMaterial> hasSubject(UUID subjectId) {
        return (root, query, cb) -> subjectId == null ? null : cb.equal(root.get("subject").get("id"), subjectId);
    }

    public static Specification<StudyMaterial> hasType(MaterialType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("materialType"), type);
    }
}
