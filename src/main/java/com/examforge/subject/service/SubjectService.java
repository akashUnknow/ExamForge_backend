package com.examforge.subject.service;

import com.examforge.subject.dto.SubjectRequest;
import com.examforge.subject.dto.SubjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubjectService {

    /** Public: only valid if the parent exam is PUBLISHED. */
    Page<SubjectResponse> getPublicByExam(UUID examId, Pageable pageable);

    /** Admin: parent exam can be in any status. */
    Page<SubjectResponse> getAdminByExam(UUID examId, Pageable pageable);

    SubjectResponse getAdminById(UUID id);

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(UUID id, SubjectRequest request);

    void delete(UUID id);
}
