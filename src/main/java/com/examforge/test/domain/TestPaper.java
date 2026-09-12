package com.examforge.test.domain;

import com.examforge.common.util.BaseEntity;
import com.examforge.exam.domain.Exam;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Named TestPaper (not Test) to avoid colliding with org.junit.jupiter.api.Test
 * when both are referenced in the same file. Maps to the "tests" table and
 * is exposed at the /api/v1/tests API path per the spec's naming.
 */
@Entity
@Table(name = "tests")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class TestPaper extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "total_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "negative_marking", nullable = false, precision = 5, scale = 2)
    private BigDecimal negativeMarking;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status = TestStatus.DRAFT;

    @Column(name = "is_free", nullable = false)
    private boolean free = true;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
}
