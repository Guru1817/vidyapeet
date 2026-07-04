package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTestId(Long testId);

    long countByTestId(Long testId);

    void deleteByTestId(Long testId);

    void deleteByInstituteId(Long instituteId);
}
