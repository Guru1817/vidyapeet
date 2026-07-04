package com.vidyapeet.attempt;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * A single answer within an attempt, with the per-question grading result. The
 * student's answer is stored in the same canonical form as the question's
 * correct answer (see {@code AnswerCodec}).
 */
@Entity
@Table(name = "attempt_answers")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class AttemptAnswer extends TenantBaseEntity {

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Null/blank when the student left the question unanswered. */
    @Column(name = "selected_answer", length = 2000)
    private String selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "marks_awarded", nullable = false)
    private Double marksAwarded = 0.0;
}
