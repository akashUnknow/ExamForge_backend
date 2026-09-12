package com.examforge.exam.service;

import com.examforge.exam.dto.ExamCategoryRequest;
import com.examforge.exam.dto.ExamCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ExamCategoryService {

    List<ExamCategoryResponse> getAll();

    ExamCategoryResponse getById(UUID id);

    ExamCategoryResponse create(ExamCategoryRequest request);

    ExamCategoryResponse update(UUID id, ExamCategoryRequest request);

    void delete(UUID id);
}
