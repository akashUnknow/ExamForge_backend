package com.examforge.exam.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.ExamCategory;
import com.examforge.exam.dto.ExamCategoryRequest;
import com.examforge.exam.dto.ExamCategoryResponse;
import com.examforge.exam.repository.ExamCategoryRepository;
import com.examforge.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamCategoryServiceImpl implements ExamCategoryService {

    private final ExamCategoryRepository categoryRepository;
    private final ExamRepository examRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExamCategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(ExamCategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamCategoryResponse getById(UUID id) {
        return ExamCategoryResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public ExamCategoryResponse create(ExamCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("An exam category named '" + name + "' already exists");
        }

        ExamCategory category = new ExamCategory();
        category.setName(name);
        category.setDescription(request.getDescription());

        return ExamCategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public ExamCategoryResponse update(UUID id, ExamCategoryRequest request) {
        ExamCategory category = findEntity(id);
        String name = request.getName().trim();

        if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("An exam category named '" + name + "' already exists");
        }

        category.setName(name);
        category.setDescription(request.getDescription());

        return ExamCategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ExamCategory category = findEntity(id);

        if (examRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete this category while exams are still assigned to it");
        }

        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
    }

    private ExamCategory findEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam category", id));
    }
}
