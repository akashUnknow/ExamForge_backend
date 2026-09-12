package com.examforge.attempt.service;

import com.examforge.attempt.domain.AttemptAnswer;
import com.examforge.attempt.domain.AttemptStatus;
import com.examforge.attempt.domain.TestAttempt;
import com.examforge.attempt.dto.AnswerRequest;
import com.examforge.attempt.dto.AttemptResponse;
import com.examforge.attempt.dto.AttemptResultResponse;
import com.examforge.attempt.dto.QuestionAttemptResponse;
import com.examforge.attempt.dto.QuestionReviewOptionResponse;
import com.examforge.attempt.dto.QuestionReviewResponse;
import com.examforge.attempt.repository.AttemptAnswerRepository;
import com.examforge.attempt.repository.TestAttemptRepository;
import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionOption;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.subscription.repository.SubscriptionRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestQuestion;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestQuestionRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptServiceImpl implements AttemptService {

    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final TestPaperRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public AttemptResponse startAttempt(UUID testId) {
        UUID currentUserId = currentUserId();

        TestPaper test = testRepository.findById(testId)
                .filter(t -> t.getStatus() == TestStatus.PUBLISHED && t.getExam().getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        if (!test.isFree() && !subscriptionRepository.hasActiveSubscription(currentUserId)) {
            throw new ForbiddenException("This test requires an active subscription");
        }

        var existing = attemptRepository.findByUserIdAndTestIdAndStatus(currentUserId, testId, AttemptStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            TestAttempt attempt = existing.get();
            boolean justEnded = checkAndAutoSubmitIfExpired(attempt);
            if (!justEnded) {
                return AttemptResponse.from(attempt);
            }
            // fall through: the resumed attempt just expired, so start a fresh one below
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUserId));

        TestAttempt attempt = new TestAttempt();
        attempt.setUser(user);
        attempt.setTest(test);
        Instant now = Instant.now();
        attempt.setStartedAt(now);
        attempt.setExpiresAt(now.plus(Duration.ofMinutes(test.getDurationMinutes())));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        return AttemptResponse.from(attemptRepository.save(attempt));
    }

    @Override
    @Transactional
    public List<QuestionAttemptResponse> getQuestions(UUID attemptId) {
        TestAttempt attempt = findOwnedAttempt(attemptId);
        checkAndAutoSubmitIfExpired(attempt);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ForbiddenException("This attempt is no longer in progress");
        }

        return testQuestionRepository.findByTestIdOrderByDisplayOrderAsc(attempt.getTest().getId()).stream()
                .map(tq -> QuestionAttemptResponse.from(tq.getQuestion(), tq.getDisplayOrder(), tq.getMarks(), tq.getNegativeMarks()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveAnswer(UUID attemptId, AnswerRequest request) {
        TestAttempt attempt = findOwnedAttempt(attemptId);
        checkAndAutoSubmitIfExpired(attempt);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ForbiddenException("This attempt has ended and can no longer be modified");
        }

        if (!testQuestionRepository.existsByTestIdAndQuestionId(attempt.getTest().getId(), request.getQuestionId())) {
            throw new BusinessException("This question is not part of this test", HttpStatus.BAD_REQUEST);
        }

        AttemptAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
                .orElseGet(() -> {
                    AttemptAnswer a = new AttemptAnswer();
                    a.setAttempt(attempt);
                    Question question = questionRepository.findById(request.getQuestionId())
                            .orElseThrow(() -> ResourceNotFoundException.of("Question", request.getQuestionId()));
                    a.setQuestion(question);
                    return a;
                });

        answer.setSelectedOptionIds(request.getSelectedOptionIds() != null ? request.getSelectedOptionIds() : Set.of());
        answer.setNumericAnswer(request.getNumericAnswer());
        answer.setMarkedForReview(request.isMarkedForReview());
        answer.setTimeSpentSeconds(request.getTimeSpentSeconds() != null ? request.getTimeSpentSeconds() : 0);
        answer.setAnsweredAt(Instant.now());

        answerRepository.save(answer);
    }

    @Override
    @Transactional
    public AttemptResultResponse submit(UUID attemptId) {
        TestAttempt attempt = findOwnedAttempt(attemptId);
        boolean justAutoSubmitted = checkAndAutoSubmitIfExpired(attempt);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            computeAndFinalize(attempt, AttemptStatus.SUBMITTED);
        } else if (!justAutoSubmitted) {
            throw new ForbiddenException("This attempt has already been submitted");
        }
        // else: it just auto-submitted as a side effect of this very call - the
        // user's intent to submit is already fulfilled, fall through to the result.

        return buildResult(attempt);
    }

    @Override
    @Transactional
    public AttemptResultResponse getResult(UUID attemptId) {
        TestAttempt attempt = findOwnedAttempt(attemptId);
        checkAndAutoSubmitIfExpired(attempt);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new ForbiddenException("The result is only available after the attempt is submitted");
        }

        return buildResult(attempt);
    }

    @Override
    @Transactional
    public int autoSubmitAllExpired() {
        List<TestAttempt> expired = attemptRepository.findByStatusAndExpiresAtBefore(
                AttemptStatus.IN_PROGRESS, Instant.now());

        for (TestAttempt attempt : expired) {
            computeAndFinalize(attempt, AttemptStatus.AUTO_SUBMITTED);
        }

        return expired.size();
    }

    /** Returns true if this call just transitioned an expired IN_PROGRESS attempt to AUTO_SUBMITTED. */
    private boolean checkAndAutoSubmitIfExpired(TestAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && attempt.isExpired()) {
            computeAndFinalize(attempt, AttemptStatus.AUTO_SUBMITTED);
            return true;
        }
        return false;
    }

    private void computeAndFinalize(TestAttempt attempt, AttemptStatus finalStatus) {
        List<TestQuestion> testQuestions = testQuestionRepository.findByTestIdOrderByDisplayOrderAsc(attempt.getTest().getId());
        Map<UUID, AttemptAnswer> answersByQuestion = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        BigDecimal totalScore = BigDecimal.ZERO;
        int correct = 0;
        int incorrect = 0;
        int unanswered = 0;

        for (TestQuestion tq : testQuestions) {
            AttemptAnswer answer = answersByQuestion.get(tq.getQuestion().getId());
            GradeResult grade = grade(tq.getQuestion(), tq.getMarks(), tq.getNegativeMarks(), answer);

            totalScore = totalScore.add(grade.marksAwarded());
            if (!grade.answered()) {
                unanswered++;
            } else if (grade.correct()) {
                correct++;
            } else {
                incorrect++;
            }
        }

        BigDecimal accuracy = (correct + incorrect) > 0
                ? BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(correct + incorrect), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        attempt.setScore(totalScore);
        attempt.setCorrectCount(correct);
        attempt.setIncorrectCount(incorrect);
        attempt.setUnansweredCount(unanswered);
        attempt.setAccuracy(accuracy);
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(finalStatus);

        attemptRepository.save(attempt);
    }

    private AttemptResultResponse buildResult(TestAttempt attempt) {
        List<TestQuestion> testQuestions = testQuestionRepository.findByTestIdOrderByDisplayOrderAsc(attempt.getTest().getId());
        Map<UUID, AttemptAnswer> answersByQuestion = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        BigDecimal maxScore = BigDecimal.ZERO;
        int timeSpent = 0;
        List<QuestionReviewResponse> reviews = new ArrayList<>();

        for (TestQuestion tq : testQuestions) {
            maxScore = maxScore.add(tq.getMarks());
            AttemptAnswer answer = answersByQuestion.get(tq.getQuestion().getId());
            GradeResult grade = grade(tq.getQuestion(), tq.getMarks(), tq.getNegativeMarks(), answer);
            if (answer != null && answer.getTimeSpentSeconds() != null) {
                timeSpent += answer.getTimeSpentSeconds();
            }
            reviews.add(buildReview(tq, answer, grade));
        }

        BigDecimal percentage = maxScore.compareTo(BigDecimal.ZERO) > 0
                ? attempt.getScore().multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getName(),
                attempt.getStatus(),
                attempt.getScore(),
                maxScore,
                percentage,
                attempt.getAccuracy(),
                attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0,
                attempt.getIncorrectCount() != null ? attempt.getIncorrectCount() : 0,
                attempt.getUnansweredCount() != null ? attempt.getUnansweredCount() : 0,
                timeSpent,
                attempt.getSubmittedAt(),
                reviews
        );
    }

    private QuestionReviewResponse buildReview(TestQuestion tq, AttemptAnswer answer, GradeResult grade) {
        Question question = tq.getQuestion();
        List<QuestionReviewOptionResponse> options = question.getOptions().stream()
                .map(QuestionReviewOptionResponse::from)
                .collect(Collectors.toList());

        return new QuestionReviewResponse(
                question.getId(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getExplanation(),
                tq.getMarks(),
                tq.getNegativeMarks(),
                options,
                answer != null ? answer.getSelectedOptionIds() : Set.of(),
                answer != null ? answer.getNumericAnswer() : null,
                question.getCorrectNumericAnswer(),
                grade.answered(),
                grade.correct(),
                grade.marksAwarded()
        );
    }

    private GradeResult grade(Question question, BigDecimal marks, BigDecimal negativeMarks, AttemptAnswer answer) {
        if (answer == null || answer.isUnanswered()) {
            return new GradeResult(false, false, BigDecimal.ZERO);
        }

        boolean correct = switch (question.getQuestionType()) {
            case NUMERIC -> answer.getNumericAnswer() != null && question.getCorrectNumericAnswer() != null
                    && answer.getNumericAnswer().compareTo(question.getCorrectNumericAnswer()) == 0;
            case MCQ, TRUE_FALSE, MULTIPLE_CHOICE -> {
                Set<UUID> correctOptionIds = question.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .map(QuestionOption::getId)
                        .collect(Collectors.toSet());
                yield correctOptionIds.equals(answer.getSelectedOptionIds());
            }
        };

        BigDecimal marksAwarded = correct ? marks : negativeMarks.negate();
        return new GradeResult(true, correct, marksAwarded);
    }

    private TestAttempt findOwnedAttempt(UUID attemptId) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attempt", attemptId));

        UUID currentUserId = currentUserId();
        if (!attempt.getUser().getId().equals(currentUserId)) {
            throw ResourceNotFoundException.of("Attempt", attemptId);
        }
        return attempt;
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ForbiddenException("Authentication is required"));
    }

    private record GradeResult(boolean answered, boolean correct, BigDecimal marksAwarded) {
    }
}
