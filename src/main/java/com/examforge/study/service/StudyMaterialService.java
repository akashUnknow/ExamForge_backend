package com.examforge.study.service;

import com.examforge.study.domain.MaterialType;
import com.examforge.study.dto.StudyMaterialResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface StudyMaterialService {

    Page<StudyMaterialResponse> search(UUID examId, UUID subjectId, MaterialType type, Pageable pageable);

    StudyMaterialResponse getById(UUID id);

    /** Loads the underlying file for download, alongside its metadata for setting response headers. */
    LoadedFile loadFile(UUID id) throws IOException;

    StudyMaterialResponse upload(String title, String description, MaterialType materialType,
                                  UUID examId, UUID subjectId, MultipartFile file) throws IOException;

    void delete(UUID id);

    record LoadedFile(Resource resource, String filename, String contentType) {
    }
}
