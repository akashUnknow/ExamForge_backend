package com.examforge.testseries.domain;

import com.examforge.common.util.BaseEntity;
import com.examforge.exam.domain.Exam;
import com.examforge.test.domain.TestStatus;
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
import java.time.LocalDate;

/**
 * A named, orderable bundle of tests under one exam. Reuses TestStatus
 * from the test module rather than a duplicate DRAFT/PUBLISHED/ARCHIVED
 * enum, since it's the identical lifecycle concept.
 */
@Entity
@Table(name = "test_series")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class TestSeries extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_free", nullable = false)
    private boolean free = true;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** How long a purchaser's access to this series lasts once granted. Null means unlimited. */
    @Column(name = "access_duration_days")
    private Integer accessDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status = TestStatus.DRAFT;
}
