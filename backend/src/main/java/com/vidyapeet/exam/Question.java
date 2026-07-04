package com.vidyapeet.exam;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * A question of any supported {@link QuestionType}. Options A–D apply only to
 * MCQ/MSQ; the correct answer is stored in canonical form (see {@link AnswerCodec}).
 */
@Entity
@Table(name = "questions")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class Question extends TenantBaseEntity {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionType type = QuestionType.MCQ;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "option_a")
    private String optionA;

    @Column(name = "option_b")
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    /** Canonical correct answer (see {@link AnswerCodec}). */
    @Column(name = "correct_answer", nullable = false, length = 2000)
    private String correctAnswer;

    @Column(nullable = false)
    private Integer marks = 1;
}
