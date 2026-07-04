package com.vidyapeet.institute;

import com.vidyapeet.attempt.repository.AttemptAnswerRepository;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.repository.BatchTestRepository;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.institute.dto.CreateInstituteRequest;
import com.vidyapeet.institute.dto.InstituteResponse;
import com.vidyapeet.institute.dto.UpdateInstituteRequest;
import com.vidyapeet.institute.repository.InstituteRepository;
import com.vidyapeet.library.repository.BatchLibraryFileRepository;
import com.vidyapeet.library.repository.LibraryFileRepository;
import com.vidyapeet.library.repository.LibraryFolderRepository;
import com.vidyapeet.note.repository.NoteRepository;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SUPER_ADMIN operations for managing coaching centers (tenants). These run with
 * tenant scoping bypassed (set for SUPER_ADMIN in the auth filter).
 */
@Service
public class InstituteService {

    private static final String DEFAULT_PRIMARY_COLOR = "#2563EB";

    private final InstituteRepository instituteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final MockTestRepository mockTestRepository;
    private final NoteRepository noteRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final BatchRepository batchRepository;
    private final BatchTestRepository batchTestRepository;
    private final BatchLibraryFileRepository batchLibraryFileRepository;
    private final LibraryFileRepository libraryFileRepository;
    private final LibraryFolderRepository libraryFolderRepository;

    public InstituteService(
            InstituteRepository instituteRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AttemptAnswerRepository attemptAnswerRepository,
            TestAttemptRepository testAttemptRepository,
            QuestionRepository questionRepository,
            MockTestRepository mockTestRepository,
            NoteRepository noteRepository,
            BatchStudentRepository batchStudentRepository,
            BatchRepository batchRepository,
            BatchTestRepository batchTestRepository,
            BatchLibraryFileRepository batchLibraryFileRepository,
            LibraryFileRepository libraryFileRepository,
            LibraryFolderRepository libraryFolderRepository) {
        this.instituteRepository = instituteRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionRepository = questionRepository;
        this.mockTestRepository = mockTestRepository;
        this.noteRepository = noteRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.batchRepository = batchRepository;
        this.batchTestRepository = batchTestRepository;
        this.batchLibraryFileRepository = batchLibraryFileRepository;
        this.libraryFileRepository = libraryFileRepository;
        this.libraryFolderRepository = libraryFolderRepository;
    }

    @Transactional
    public InstituteResponse create(CreateInstituteRequest request) {
        if (instituteRepository.existsBySlug(request.slug())) {
            throw Exceptions.conflict("Slug '" + request.slug() + "' is already taken.");
        }

        Institute institute = new Institute();
        institute.setName(request.name());
        institute.setSlug(request.slug());
        institute.setLogoUrl(request.logoUrl());
        institute.setPrimaryColor(
                StringUtils.hasText(request.primaryColor()) ? request.primaryColor() : DEFAULT_PRIMARY_COLOR);
        institute = instituteRepository.save(institute);

        // Create the center's first admin account.
        if (userRepository.existsByInstituteIdAndEmail(institute.getId(), request.adminEmail())) {
            throw Exceptions.conflict("An admin with this email already exists for this institute.");
        }
        User admin = new User();
        admin.setInstituteId(institute.getId());
        admin.setName(request.adminName());
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setRole(Role.INSTITUTE_ADMIN);
        userRepository.save(admin);

        return InstituteResponse.from(institute);
    }

    @Transactional(readOnly = true)
    public List<InstituteResponse> list() {
        return instituteRepository.findAll().stream()
                .map(InstituteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstituteResponse get(Long id) {
        return InstituteResponse.from(findOrThrow(id));
    }

    @Transactional
    public InstituteResponse update(Long id, UpdateInstituteRequest request) {
        Institute institute = findOrThrow(id);
        institute.setName(request.name());
        institute.setLogoUrl(request.logoUrl());
        if (StringUtils.hasText(request.primaryColor())) {
            institute.setPrimaryColor(request.primaryColor());
        }
        return InstituteResponse.from(instituteRepository.save(institute));
    }

    private Institute findOrThrow(Long id) {
        return instituteRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No institute found with id " + id + "."));
    }

    /**
     * Permanently deletes an institute and all of its data (students, batches,
     * notes, tests, questions, attempts). Runs in dependency order. Destructive
     * and irreversible — restricted to SUPER_ADMIN at the controller.
     */
    @Transactional
    public void delete(Long id) {
        Institute institute = findOrThrow(id);
        Long instituteId = institute.getId();

        attemptAnswerRepository.deleteByInstituteId(instituteId);
        testAttemptRepository.deleteByInstituteId(instituteId);
        batchTestRepository.deleteByInstituteId(instituteId);
        questionRepository.deleteByInstituteId(instituteId);
        mockTestRepository.deleteByInstituteId(instituteId);
        batchLibraryFileRepository.deleteByInstituteId(instituteId);
        libraryFileRepository.deleteByInstituteId(instituteId);
        libraryFolderRepository.deleteByInstituteId(instituteId);
        noteRepository.deleteByInstituteId(instituteId);
        batchStudentRepository.deleteByInstituteId(instituteId);
        batchRepository.deleteByInstituteId(instituteId);
        userRepository.deleteByInstituteId(instituteId);
        instituteRepository.delete(institute);
    }
}
