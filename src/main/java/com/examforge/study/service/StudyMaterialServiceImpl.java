package com.examforge.study.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.storage.FileStorageService;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.study.domain.MaterialType;
import com.examforge.study.domain.StudyMaterial;
import com.examforge.study.dto.StudyMaterialResponse;
import com.examforge.study.repository.StudyMaterialRepository;
import com.examforge.study.repository.StudyMaterialSpecifications;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyMaterialServiceImpl implements StudyMaterialService {

    private final StudyMaterialRepository materialRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<StudyMaterialResponse> search(UUID examId, UUID subjectId, MaterialType type, Pageable pageable) {
        Specification<StudyMaterial> spec = Specification
                .where(StudyMaterialSpecifications.hasExam(examId))
                .and(StudyMaterialSpecifications.hasSubject(subjectId))
                .and(StudyMaterialSpecifications.hasType(type));

        return materialRepository.findAll(spec, pageable).map(StudyMaterialResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyMaterialResponse getById(UUID id) {
        return StudyMaterialResponse.from(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public LoadedFile loadFile(UUID id) throws IOException {
        StudyMaterial material = findEntity(id);
        return new LoadedFile(
                fileStorageService.load(material.getStorageKey()),
                material.getOriginalFilename(),
                material.getContentType()
        );
    }

    @Override
    @Transactional
    public StudyMaterialResponse upload(String title, String description, MaterialType materialType,
                                         UUID examId, UUID subjectId, MultipartFile file) throws IOException {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Title is required", HttpStatus.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("A file is required", HttpStatus.BAD_REQUEST);
        }
        if (materialType == null) {
            throw new BusinessException("Material type is required", HttpStatus.BAD_REQUEST);
        }

        Exam exam = null;
        if (examId != null) {
            exam = examRepository.findById(examId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Exam", examId));
        }
        Subject subject = null;
        if (subjectId != null) {
            subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Subject", subjectId));
        }

        FileStorageService.StoredFile stored = fileStorageService.store(
                file.getOriginalFilename(), file.getContentType(), file.getInputStream(), file.getSize());

        StudyMaterial material = new StudyMaterial();
        material.setTitle(title.trim());
        material.setDescription(description);
        material.setMaterialType(materialType);
        material.setExam(exam);
        material.setSubject(subject);
        material.setStorageKey(stored.storageKey());
        material.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        material.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        material.setFileSizeBytes(stored.sizeBytes());

        return StudyMaterialResponse.from(materialRepository.save(material));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        StudyMaterial material = findEntity(id);
        fileStorageService.delete(material.getStorageKey());
        material.setDeletedAt(Instant.now());
        materialRepository.save(material);
    }

    private StudyMaterial findEntity(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Study material", id));
    }
}
