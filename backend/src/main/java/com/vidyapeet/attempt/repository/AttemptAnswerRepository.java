package com.vidyapeet.attempt.repository;

import com.vidyapeet.attempt.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(Long attemptId);

    void deleteByAttemptIdIn(List<Long> attemptIds);

    void deleteByInstituteId(Long instituteId);
}
