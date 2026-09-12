package com.examforge.study.dto;

import com.examforge.study.domain.MaterialType;
import com.examforge.study.domain.StudyMaterial;

import java.time.Instant;
import java.util.UUID;

public record StudyMaterialResponse(
        UUID id,
        String title,
        String description,
        MaterialType materialType,
        UUID examId,
        UUID subjectId,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        String downloadUrl,
        Instant createdAt
) {
    public static StudyMaterialResponse from(StudyMaterial material) {
        return new StudyMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getMaterialType(),
                material.getExam() != null ? material.getExam().getId() : null,
                material.getSubject() != null ? material.getSubject().getId() : null,
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSizeBytes(),
                "/api/v1/study-materials/" + material.getId() + "/file",
                material.getCreatedAt()
        );
    }
}
