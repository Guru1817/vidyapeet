package com.vidyapeet.batch;

import com.vidyapeet.batch.dto.BatchResponse;
import com.vidyapeet.batch.dto.CreateBatchRequest;
import com.vidyapeet.batch.dto.UpdateBatchRequest;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.user.StudentAdminService;
import com.vidyapeet.user.User;
import com.vidyapeet.user.dto.StudentResponse;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * INSTITUTE_ADMIN management of batches and enrollment. All batch queries are
 * tenant-scoped automatically; student references are validated to belong to the
 * same institute.
 */
@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final StudentAdminService studentAdminService;
    private final UserRepository userRepository;

    public BatchService(
            BatchRepository batchRepository,
            BatchStudentRepository batchStudentRepository,
            StudentAdminService studentAdminService,
            UserRepository userRepository) {
        this.batchRepository = batchRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.studentAdminService = studentAdminService;
        this.userRepository = userRepository;
    }

    @Transactional
    public BatchResponse create(CreateBatchRequest request) {
        Batch batch = new Batch();
        batch.setName(request.name());
        batch.setDescription(request.description());
        batch = batchRepository.save(batch);
        return BatchResponse.from(batch, 0);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> list() {
        return batchRepository.findAllByOrderByNameAsc().stream()
                .map(b -> BatchResponse.from(b, batchStudentRepository.countByBatchId(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse get(Long id) {
        Batch batch = requireBatch(id);
        return BatchResponse.from(batch, batchStudentRepository.countByBatchId(id));
    }

    @Transactional
    public BatchResponse update(Long id, UpdateBatchRequest request) {
        Batch batch = requireBatch(id);
        batch.setName(request.name());
        batch.setDescription(request.description());
        batch = batchRepository.save(batch);
        return BatchResponse.from(batch, batchStudentRepository.countByBatchId(id));
    }

    @Transactional
    public void enroll(Long batchId, Long studentId) {
        requireBatch(batchId);
        studentAdminService.requireStudentInCurrentInstitute(studentId);
        if (batchStudentRepository.existsByBatchIdAndStudentId(batchId, studentId)) {
            throw Exceptions.conflict("Student is already enrolled in this batch.");
        }
        BatchStudent enrollment = new BatchStudent();
        enrollment.setBatchId(batchId);
        enrollment.setStudentId(studentId);
        batchStudentRepository.save(enrollment);
    }

    @Transactional
    public void unenroll(Long batchId, Long studentId) {
        requireBatch(batchId);
        BatchStudent enrollment = batchStudentRepository.findByBatchIdAndStudentId(batchId, studentId)
                .orElseThrow(() -> Exceptions.notFound("Student is not enrolled in this batch."));
        batchStudentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(Long batchId) {
        requireBatch(batchId);
        List<Long> studentIds = batchStudentRepository.findByBatchId(batchId).stream()
                .map(BatchStudent::getStudentId)
                .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(studentIds).stream()
                .map(StudentResponse::from)
                .toList();
    }

    private Batch requireBatch(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No batch found with id " + id + "."));
    }
}
