package com.examforge.analytics.service;

import com.examforge.analytics.dto.PopularExamResponse;
import com.examforge.analytics.dto.PopularTestResponse;
import com.examforge.analytics.dto.RegistrationTrendPoint;
import com.examforge.analytics.dto.TestAnalyticsResponse;
import com.examforge.attempt.repository.TestAttemptRepository;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregation queries backing the admin analytics dashboard. Every method
 * is cached (Redis, ~10 min TTL via the default cache config from
 * RedisConfig) since dashboard data doesn't need millisecond freshness and
 * these are exactly the kind of query that shouldn't hit Postgres on every
 * page load.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TestAttemptRepository attemptRepository;
    private final TestPaperRepository testRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics:test", key = "#testId")
    public TestAnalyticsResponse getTestAnalytics(UUID testId) {
        TestPaper test = testRepository.findById(testId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        long total = attemptRepository.countByTestId(testId);
        long completed = attemptRepository.countCompletedByTestId(testId);
        long inProgress = total - completed;

        BigDecimal averageScore = nullToZero(attemptRepository.averageScoreByTestId(testId));
        BigDecimal averageAccuracy = nullToZero(attemptRepository.averageAccuracyByTestId(testId));
        BigDecimal completionRate = total > 0
                ? BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TestAnalyticsResponse(
                test.getId(), test.getName(), total, completed, inProgress,
                averageScore, averageAccuracy, completionRate);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics:popularTests", key = "#limit")
    public List<PopularTestResponse> getPopularTests(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return attemptRepository.findPopularTests(pageable).stream()
                .map(p -> new PopularTestResponse(p.getTestId(), p.getTestName(), p.getAttemptCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics:popularExams", key = "#limit")
    public List<PopularExamResponse> getPopularExams(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return attemptRepository.findPopularExams(pageable).stream()
                .map(p -> new PopularExamResponse(p.getExamId(), p.getExamName(), p.getAttemptCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics:registrationTrend", key = "#days")
    public List<RegistrationTrendPoint> getRegistrationTrend(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return userRepository.findRegistrationTrend(since).stream()
                .map(p -> new RegistrationTrendPoint(p.getDay(), p.getCount()))
                .collect(Collectors.toList());
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
