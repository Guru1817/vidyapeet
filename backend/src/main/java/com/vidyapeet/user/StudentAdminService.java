package com.vidyapeet.user;

import com.vidyapeet.attempt.TestAttempt;
import com.vidyapeet.attempt.repository.AttemptAnswerRepository;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.user.dto.CreateStudentRequest;
import com.vidyapeet.user.dto.StudentResponse;
import com.vidyapeet.user.dto.UpdateStudentRequest;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * INSTITUTE_ADMIN management of student accounts within the admin's own
 * institute. Students are {@link User}s with role STUDENT.
 */
@Service
public class StudentAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BatchStudentRepository batchStudentRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public StudentAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BatchStudentRepository batchStudentRepository,
            TestAttemptRepository testAttemptRepository,
            AttemptAnswerRepository attemptAnswerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.batchStudentRepository = batchStudentRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        Long instituteId = SecurityUtils.currentInstituteId();
        if (userRepository.existsByInstituteIdAndEmail(instituteId, request.email())) {
            throw Exceptions.conflict("A user with this email already exists in your institute.");
        }
        User student = new User();
        student.setInstituteId(instituteId);
        student.setName(request.name());
        student.setEmail(request.email());
        student.setPasswordHash(passwordEncoder.encode(request.password()));
        student.setRole(Role.STUDENT);
        student.setDescription(request.description());
        return StudentResponse.from(userRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long studentId, UpdateStudentRequest request) {
        User student = requireStudentInCurrentInstitute(studentId);

        // If email changed, ensure it's unique within the institute.
        if (!student.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByInstituteIdAndEmail(student.getInstituteId(), request.email())) {
            throw Exceptions.conflict("A user with this email already exists in your institute.");
        }
        student.setName(request.name());
        student.setEmail(request.email());
        student.setDescription(request.description());
        if (StringUtils.hasText(request.password())) {
            student.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return StudentResponse.from(userRepository.save(student));
    }

    @Transactional
    public void delete(Long studentId) {
        User student = requireStudentInCurrentInstitute(studentId);

        // Remove dependent rows first (attempt answers -> attempts -> enrollments).
        List<Long> attemptIds = testAttemptRepository.findByStudentId(studentId).stream()
                .map(TestAttempt::getId)
                .toList();
        if (!attemptIds.isEmpty()) {
            attemptAnswerRepository.deleteByAttemptIdIn(attemptIds);
        }
        testAttemptRepository.deleteByStudentId(studentId);
        batchStudentRepository.deleteByStudentId(studentId);
        userRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> list() {
        Long instituteId = SecurityUtils.currentInstituteId();
        return userRepository.findByInstituteIdAndRole(instituteId, Role.STUDENT).stream()
                .map(StudentResponse::from)
                .toList();
    }

    /**
     * Loads a student in the caller's institute, enforcing that the user exists,
     * is a STUDENT, and belongs to the same institute.
     */
    @Transactional(readOnly = true)
    public User requireStudentInCurrentInstitute(Long studentId) {
        Long instituteId = SecurityUtils.currentInstituteId();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> Exceptions.notFound("No student found with id " + studentId + "."));
        if (!instituteId.equals(student.getInstituteId()) || student.getRole() != Role.STUDENT) {
            // Do not reveal cross-tenant existence.
            throw Exceptions.notFound("No student found with id " + studentId + ".");
        }
        return student;
    }
}
