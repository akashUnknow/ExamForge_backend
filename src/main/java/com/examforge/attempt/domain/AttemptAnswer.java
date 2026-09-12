package com.examforge.attempt.domain;

import com.examforge.question.domain.Question;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A user's answer to one question within one attempt. selectedOptionIds
 * generalizes the spec's singular "selectedOption" field to a set, since
 * the question bank also supports MULTIPLE_CHOICE (multiple correct
 * options) - MCQ/TRUE_FALSE answers simply end up with a single-element set.
 */
@Entity
@Table(name = "attempt_answers")
@Getter
@Setter
@NoArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attempt_answer_selected_options", joinColumns = @JoinColumn(name = "attempt_answer_id"))
    @Column(name = "option_id")
    private Set<UUID> selectedOptionIds = new HashSet<>();

    /** Only meaningful for NUMERIC questions. */
    @Column(name = "numeric_answer", precision = 12, scale = 4)
    private BigDecimal numericAnswer;

    @Column(name = "marked_for_review", nullable = false)
    private boolean markedForReview = false;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "time_spent_seconds", nullable = false)
    private Integer timeSpentSeconds = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isUnanswered() {
        return selectedOptionIds.isEmpty() && numericAnswer == null;
    }
}
