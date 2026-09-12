package com.examforge.subject.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.dto.SubjectRequest;
import com.examforge.subject.dto.SubjectResponse;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final TopicRepository topicRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SubjectResponse> getPublicByExam(UUID examId, Pageable pageable) {
        Exam exam = examRepository.findById(examId)
                .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", examId));

        return subjectRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId(), pageable)
                .map(SubjectResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubjectResponse> getAdminByExam(UUID examId, Pageable pageable) {
        if (!examRepository.existsById(examId)) {
            throw ResourceNotFoundException.of("Exam", examId);
        }
        return subjectRepository.findByExamIdOrderByDisplayOrderAsc(examId, pageable)
                .map(SubjectResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getAdminById(UUID id) {
        return SubjectResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));

        String name = request.getName().trim();
        if (subjectRepository.existsByExamIdAndNameIgnoreCase(exam.getId(), name)) {
            throw new DuplicateResourceException("A subject named '" + name + "' already exists for this exam");
        }

        Subject subject = new Subject();
        subject.setExam(exam);
        subject.setName(name);
        subject.setDescription(request.getDescription());
        subject.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(UUID id, SubjectRequest request) {
        Subject subject = findEntity(id);

        Exam exam = subject.getExam();
        if (!exam.getId().equals(request.getExamId())) {
            exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));
        }

        String name = request.getName().trim();
        boolean nameOrExamChanged = !name.equalsIgnoreCase(subject.getName()) || !exam.getId().equals(subject.getExam().getId());
        if (nameOrExamChanged && subjectRepository.existsByExamIdAndNameIgnoreCase(exam.getId(), name)) {
            throw new DuplicateResourceException("A subject named '" + name + "' already exists for this exam");
        }

        subject.setExam(exam);
        subject.setName(name);
        subject.setDescription(request.getDescription());
        subject.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : subject.getDisplayOrder());

        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Subject subject = findEntity(id);

        if (topicRepository.existsBySubjectId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete this subject while topics are still attached to it");
        }

        subject.setDeletedAt(Instant.now());
        subjectRepository.save(subject);
    }

    private Subject findEntity(UUID id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", id));
    }
}
