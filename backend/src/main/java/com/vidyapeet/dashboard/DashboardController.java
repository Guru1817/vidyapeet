package com.vidyapeet.dashboard;

import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.note.repository.NoteRepository;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Summary counts for the institute admin dashboard. All counts are tenant-scoped
 * automatically (the admin's institute), except the student count which is keyed
 * by role.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('INSTITUTE_ADMIN')")
public class DashboardController {

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final MockTestRepository mockTestRepository;
    private final NoteRepository noteRepository;

    public DashboardController(
            UserRepository userRepository,
            BatchRepository batchRepository,
            MockTestRepository mockTestRepository,
            NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.mockTestRepository = mockTestRepository;
        this.noteRepository = noteRepository;
    }

    public record DashboardStats(long students, long batches, long tests, long notes) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardStats stats() {
        Long instituteId = SecurityUtils.currentInstituteId();
        return new DashboardStats(
                userRepository.countByInstituteIdAndRole(instituteId, Role.STUDENT),
                batchRepository.count(),
                mockTestRepository.count(),
                noteRepository.count());
    }
}
