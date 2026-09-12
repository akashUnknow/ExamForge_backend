package com.examforge.attempt.repository;

import com.examforge.attempt.domain.AttemptStatus;
import com.examforge.attempt.domain.TestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {

    Optional<TestAttempt> findByUserIdAndTestIdAndStatus(UUID userId, UUID testId, AttemptStatus status);

    Page<TestAttempt> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Page<TestAttempt> findByUserIdAndTestIdOrderByStartedAtDesc(UUID userId, UUID testId, Pageable pageable);

    /** Used by the background sweep - IN_PROGRESS attempts whose deadline has already passed. */
    List<TestAttempt> findByStatusAndExpiresAtBefore(AttemptStatus status, Instant instant);

    // ---- Analytics aggregation (see analytics.repository.AnalyticsRepository for cross-entity queries) ----

    long countByTestId(UUID testId);

    @Query("select count(a) from TestAttempt a where a.test.id = :testId and a.status in ('SUBMITTED', 'AUTO_SUBMITTED')")
    long countCompletedByTestId(@Param("testId") UUID testId);

    @Query("select avg(a.score) from TestAttempt a where a.test.id = :testId and a.status in ('SUBMITTED', 'AUTO_SUBMITTED')")
    BigDecimal averageScoreByTestId(@Param("testId") UUID testId);

    @Query("select avg(a.accuracy) from TestAttempt a where a.test.id = :testId and a.status in ('SUBMITTED', 'AUTO_SUBMITTED')")
    BigDecimal averageAccuracyByTestId(@Param("testId") UUID testId);

    @Query("select a.test.id as testId, a.test.name as testName, count(a) as attemptCount " +
            "from TestAttempt a group by a.test.id, a.test.name order by count(a) desc")
    List<PopularTestProjection> findPopularTests(Pageable pageable);

    @Query("select a.test.exam.id as examId, a.test.exam.name as examName, count(a) as attemptCount " +
            "from TestAttempt a group by a.test.exam.id, a.test.exam.name order by count(a) desc")
    List<PopularExamProjection> findPopularExams(Pageable pageable);

    interface PopularTestProjection {
        UUID getTestId();
        String getTestName();
        Long getAttemptCount();
    }

    interface PopularExamProjection {
        UUID getExamId();
        String getExamName();
        Long getAttemptCount();
    }

    // ---- Ranking: best (highest-scoring) completed attempt per user for a test ----

    @Query(value = """
            SELECT * FROM (
                SELECT DISTINCT ON (ta.user_id) ta.*
                FROM test_attempts ta
                WHERE ta.test_id = :testId AND ta.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                ORDER BY ta.user_id, ta.score DESC
            ) best
            ORDER BY best.score DESC, best.accuracy DESC NULLS LAST
            """,
            countQuery = """
            SELECT count(DISTINCT ta.user_id) FROM test_attempts ta
            WHERE ta.test_id = :testId AND ta.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
            """,
            nativeQuery = true)
    Page<TestAttempt> findBestAttemptPerUser(@Param("testId") UUID testId, Pageable pageable);
}
