package com.examforge.test.domain;

import com.examforge.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "test_sections")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class TestSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private TestPaper test;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Nullable - a section without its own timer uses the whole test's duration. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal marks;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
