package com.examforge.question.service;

import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionOption;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.dto.QuestionOptionRequest;
import com.examforge.question.dto.QuestionRequest;
import com.examforge.question.dto.QuestionResponse;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.question.repository.QuestionSpecifications;
import com.examforge.question.validation.QuestionAnswerValidator;
import com.examforge.topic.domain.Topic;
import com.examforge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enforces the content workflow: DRAFT/REJECTED are the only editable
 * states; state transitions (submit/approve/reject/publish/archive) each
 * check both the caller's role and, where relevant, ownership - a
 * CONTENT_CREATOR may only touch their own questions, a REVIEWER may not
 * review their own submissions, and ADMIN/SUPER_ADMIN can act on anything
 * but still must follow the same state machine (no shortcutting straight
 * to PUBLISHED) so published content always went through review.
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final QuestionAnswerValidator answerValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> search(UUID topicId, UUID subjectId, QuestionStatus status,
                                          ExamDifficulty difficulty, QuestionType type, String keyword, Pageable pageable) {
        Specification<Question> spec = Specification
                .where(QuestionSpecifications.hasTopic(topicId))
                .and(QuestionSpecifications.hasSubject(subjectId))
                .and(QuestionSpecifications.hasStatus(status))
                .and(QuestionSpecifications.hasDifficulty(difficulty))
                .and(QuestionSpecifications.hasType(type))
                .and(QuestionSpecifications.textContains(keyword));

        return questionRepository.findAll(spec, pageable).map(QuestionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getById(UUID id) {
        return QuestionResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> ResourceNotFoundException.of("Topic", request.getTopicId()));

        answerValidator.validate(request.getQuestionType(), request.getOptions(), request.getCorrectNumericAnswer());

        Question question = new Question();
        question.setTopic(topic);
        question.setStatus(QuestionStatus.DRAFT);
        applyRequest(question, request);

        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse update(UUID id, QuestionRequest request) {
        Question question = findEntity(id);
        assertCanEdit(question);

        if (question.getStatus() != QuestionStatus.DRAFT && question.getStatus() != QuestionStatus.REJECTED) {
            throw new ForbiddenException("Only questions in DRAFT or REJECTED status can be edited");
        }

        answerValidator.validate(request.getQuestionType(), request.getOptions(), request.getCorrectNumericAnswer());

        if (!question.getTopic().getId().equals(request.getTopicId())) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Topic", request.getTopicId()));
            question.setTopic(topic);
        }

        applyRequest(question, request);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Question question = findEntity(id);
        assertCanEdit(question);

        if (question.getStatus() != QuestionStatus.DRAFT) {
            throw new ForbiddenException("Only DRAFT questions can be deleted - archive it instead");
        }

        question.setDeletedAt(Instant.now());
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public QuestionResponse submitForReview(UUID id) {
        Question question = findEntity(id);
        assertCanEdit(question);
        assertStatus(question, "submit for review", QuestionStatus.DRAFT, QuestionStatus.REJECTED);

        question.setStatus(QuestionStatus.IN_REVIEW);
        question.setRejectionReason(null);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse approve(UUID id) {
        Question question = findEntity(id);
        assertCanReview(question);
        assertStatus(question, "approve", QuestionStatus.IN_REVIEW);

        question.setStatus(QuestionStatus.APPROVED);
        question.setRejectionReason(null);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse reject(UUID id, String reason) {
        Question question = findEntity(id);
        assertCanReview(question);
        assertStatus(question, "reject", QuestionStatus.IN_REVIEW);

        question.setStatus(QuestionStatus.REJECTED);
        question.setRejectionReason(reason);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse publish(UUID id) {
        Question question = findEntity(id);
        assertStatus(question, "publish", QuestionStatus.APPROVED);

        question.setStatus(QuestionStatus.PUBLISHED);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional
    public QuestionResponse archive(UUID id) {
        Question question = findEntity(id);
        assertStatus(question, "archive", QuestionStatus.PUBLISHED, QuestionStatus.APPROVED, QuestionStatus.REJECTED);

        question.setStatus(QuestionStatus.ARCHIVED);
        return QuestionResponse.from(questionRepository.save(question));
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setQuestionType(request.getQuestionType());
        question.setQuestionText(request.getQuestionText());
        question.setExplanation(request.getExplanation());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks());
        question.setNegativeMarks(request.getNegativeMarks());
        question.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en");

        if (request.getQuestionType() == QuestionType.NUMERIC) {
            question.setCorrectNumericAnswer(request.getCorrectNumericAnswer());
            question.replaceOptions(List.of());
        } else {
            question.setCorrectNumericAnswer(null);
            question.replaceOptions(toOptionEntities(request.getOptions()));
        }

        Set<String> tags = request.getTags() != null ? request.getTags() : Set.of();
        question.setTags(tags.stream().map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toSet()));
    }

    private List<QuestionOption> toOptionEntities(List<QuestionOptionRequest> requests) {
        List<QuestionOption> result = new ArrayList<>();
        for (QuestionOptionRequest req : requests) {
            result.add(new QuestionOption(req.getOptionText().trim(), req.isCorrect(),
                    req.getDisplayOrder() != null ? req.getDisplayOrder() : 0));
        }
        return result;
    }

    /** CONTENT_CREATOR may only edit their own questions; ADMIN/SUPER_ADMIN may edit any. */
    private void assertCanEdit(Question question) {
        if (SecurityUtils.currentUserHasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        String currentEmail = SecurityUtils.getCurrentUserEmail().orElse(null);
        if (currentEmail == null || !currentEmail.equalsIgnoreCase(question.getCreatedBy())) {
            throw new ForbiddenException("You can only modify questions you created");
        }
    }

    /** REVIEWER may not review their own submissions; ADMIN/SUPER_ADMIN have no such restriction. */
    private void assertCanReview(Question question) {
        if (SecurityUtils.currentUserHasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        String currentEmail = SecurityUtils.getCurrentUserEmail().orElse(null);
        if (currentEmail != null && currentEmail.equalsIgnoreCase(question.getCreatedBy())) {
            throw new ForbiddenException("You cannot review your own submission");
        }
    }

    private void assertStatus(Question question, String action, QuestionStatus... allowed) {
        for (QuestionStatus status : allowed) {
            if (question.getStatus() == status) {
                return;
            }
        }
        throw new ForbiddenException("Cannot " + action + " a question in " + question.getStatus() + " status");
    }

    private Question findEntity(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Question", id));
    }
}
