package com.examforge.test.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.question.repository.QuestionSpecifications;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestQuestion;
import com.examforge.test.domain.TestSection;
import com.examforge.test.dto.AutoGenerateBucket;
import com.examforge.test.dto.AutoGenerateRequest;
import com.examforge.test.dto.TestQuestionAttachRequest;
import com.examforge.test.dto.TestQuestionResponse;
import com.examforge.test.dto.TestQuestionUpdateRequest;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestQuestionRepository;
import com.examforge.test.repository.TestSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestQuestionServiceImpl implements TestQuestionService {

    private final TestQuestionRepository testQuestionRepository;
    private final TestPaperRepository testRepository;
    private final TestSectionRepository sectionRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TestQuestionResponse> getByTest(UUID testId) {
        if (!testRepository.existsById(testId)) {
            throw ResourceNotFoundException.of("Test", testId);
        }
        return testQuestionRepository.findByTestIdOrderByDisplayOrderAsc(testId).stream()
                .map(TestQuestionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestQuestionResponse attach(UUID testId, TestQuestionAttachRequest request) {
        TestPaper test = testRepository.findById(testId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Question", request.getQuestionId()));

        requirePublished(question);

        if (testQuestionRepository.existsByTestIdAndQuestionId(testId, question.getId())) {
            throw new DuplicateResourceException("This question is already attached to the test");
        }

        TestSection section = resolveSection(test, request.getSectionId());

        TestQuestion testQuestion = new TestQuestion();
        testQuestion.setTest(test);
        testQuestion.setQuestion(question);
        testQuestion.setSection(section);
        testQuestion.setMarks(request.getMarks() != null ? request.getMarks() : question.getMarks());
        testQuestion.setNegativeMarks(request.getNegativeMarks() != null ? request.getNegativeMarks() : question.getNegativeMarks());
        testQuestion.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextDisplayOrder(testId));

        return TestQuestionResponse.from(testQuestionRepository.save(testQuestion));
    }

    @Override
    @Transactional
    public TestQuestionResponse update(UUID testId, UUID testQuestionId, TestQuestionUpdateRequest request) {
        TestQuestion testQuestion = findEntity(testId, testQuestionId);
        TestSection section = resolveSection(testQuestion.getTest(), request.getSectionId());

        testQuestion.setSection(section);
        testQuestion.setDisplayOrder(request.getDisplayOrder());
        testQuestion.setMarks(request.getMarks());
        testQuestion.setNegativeMarks(request.getNegativeMarks());

        return TestQuestionResponse.from(testQuestionRepository.save(testQuestion));
    }

    @Override
    @Transactional
    public void detach(UUID testId, UUID testQuestionId) {
        TestQuestion testQuestion = findEntity(testId, testQuestionId);
        testQuestionRepository.delete(testQuestion);
    }

    @Override
    @Transactional
    public List<TestQuestionResponse> autoGenerate(UUID testId, AutoGenerateRequest request) {
        TestPaper test = testRepository.findById(testId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        TestSection section = resolveSection(test, request.getSectionId());

        Set<UUID> alreadyAttached = testQuestionRepository.findQuestionIdsByTestId(testId);
        List<UUID> usedThisRun = new ArrayList<>(alreadyAttached);
        int displayOrder = nextDisplayOrder(testId);

        List<TestQuestion> toSave = new ArrayList<>();

        for (AutoGenerateBucket bucket : request.getBuckets()) {
            if (bucket.getTopicId() == null && bucket.getSubjectId() == null) {
                throw new BusinessException(
                        "Each bucket must specify either a topicId or a subjectId", HttpStatus.BAD_REQUEST);
            }

            Specification<Question> spec = Specification
                    .where(QuestionSpecifications.hasStatus(QuestionStatus.PUBLISHED))
                    .and(QuestionSpecifications.hasDifficulty(bucket.getDifficulty()))
                    .and(bucket.getTopicId() != null
                            ? QuestionSpecifications.hasTopic(bucket.getTopicId())
                            : QuestionSpecifications.hasSubject(bucket.getSubjectId()));

            List<Question> candidates = questionRepository.findAll(spec).stream()
                    .filter(q -> !usedThisRun.contains(q.getId()))
                    .collect(Collectors.toList());

            if (candidates.size() < bucket.getCount()) {
                throw new BusinessException(String.format(
                        "Not enough PUBLISHED questions available for difficulty %s: requested %d, found %d. "
                                + "No questions were added.",
                        bucket.getDifficulty(), bucket.getCount(), candidates.size()), HttpStatus.CONFLICT);
            }

            Collections.shuffle(candidates);
            List<Question> selected = candidates.subList(0, bucket.getCount());

            for (Question question : selected) {
                TestQuestion testQuestion = new TestQuestion();
                testQuestion.setTest(test);
                testQuestion.setQuestion(question);
                testQuestion.setSection(section);
                testQuestion.setMarks(question.getMarks());
                testQuestion.setNegativeMarks(question.getNegativeMarks());
                testQuestion.setDisplayOrder(displayOrder++);
                toSave.add(testQuestion);
                usedThisRun.add(question.getId());
            }
        }

        return testQuestionRepository.saveAll(toSave).stream()
                .map(TestQuestionResponse::from)
                .collect(Collectors.toList());
    }

    private void requirePublished(Question question) {
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BusinessException("Only PUBLISHED questions can be added to a test", HttpStatus.BAD_REQUEST);
        }
    }

    private TestSection resolveSection(TestPaper test, UUID sectionId) {
        if (sectionId == null) {
            return null;
        }
        TestSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test section", sectionId));
        if (!section.getTest().getId().equals(test.getId())) {
            throw new BusinessException("That section does not belong to this test", HttpStatus.BAD_REQUEST);
        }
        return section;
    }

    private int nextDisplayOrder(UUID testId) {
        return (int) testQuestionRepository.countByTestId(testId) + 1;
    }

    private TestQuestion findEntity(UUID testId, UUID testQuestionId) {
        TestQuestion testQuestion = testQuestionRepository.findById(testQuestionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test question", testQuestionId));
        if (!testQuestion.getTest().getId().equals(testId)) {
            throw ResourceNotFoundException.of("Test question", testQuestionId);
        }
        return testQuestion;
    }
}
