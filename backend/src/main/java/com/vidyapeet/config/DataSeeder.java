package com.vidyapeet.config;

import com.vidyapeet.batch.Batch;
import com.vidyapeet.batch.BatchStudent;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.exam.AnswerOption;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.institute.Institute;
import com.vidyapeet.institute.repository.InstituteRepository;
import com.vidyapeet.note.Note;
import com.vidyapeet.note.repository.NoteRepository;
import com.vidyapeet.tenant.TenantContext;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a demo institute so the platform is usable immediately. Idempotent:
 * skips if the demo institute already exists. Enabled via
 * {@code vidyapeet.seed.enabled=true} (default in the dev profile).
 */
@Component
@ConditionalOnProperty(name = "vidyapeet.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_SLUG = "demo";

    private final InstituteRepository instituteRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final NoteRepository noteRepository;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            InstituteRepository instituteRepository,
            UserRepository userRepository,
            BatchRepository batchRepository,
            BatchStudentRepository batchStudentRepository,
            NoteRepository noteRepository,
            MockTestRepository mockTestRepository,
            QuestionRepository questionRepository,
            PasswordEncoder passwordEncoder) {
        this.instituteRepository = instituteRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.noteRepository = noteRepository;
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (instituteRepository.existsBySlug(DEMO_SLUG)) {
            log.info("Demo data already present; skipping seed.");
            return;
        }

        // Platform owner (no institute).
        createSuperAdmin();

        // Demo institute (tenant).
        Institute institute = new Institute();
        institute.setName("Vidyapeet Demo Classes");
        institute.setSlug(DEMO_SLUG);
        institute.setPrimaryColor("#4F46E5");
        institute.setLogoUrl(null);
        institute = instituteRepository.save(institute);

        // Scope subsequent tenant-entity inserts to the demo institute.
        TenantContext.setTenantId(institute.getId());
        try {
            seedTenantData(institute);
        } finally {
            TenantContext.clear();
        }

        log.info("Seeded demo institute '{}' (slug='{}').", institute.getName(), DEMO_SLUG);
        log.info("Logins -> super admin: superadmin@vidyapeet.app / superadmin123 | "
                + "admin: admin@demo.test / admin12345 | student: student1@demo.test / student123");
    }

    private void createSuperAdmin() {
        if (!userRepository.existsByEmailAndInstituteIdIsNull("superadmin@vidyapeet.app")) {
            User superAdmin = new User();
            superAdmin.setName("Platform Owner");
            superAdmin.setEmail("superadmin@vidyapeet.app");
            superAdmin.setPasswordHash(passwordEncoder.encode("superadmin123"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            userRepository.save(superAdmin);
        }
    }

    private void seedTenantData(Institute institute) {
        Long instituteId = institute.getId();

        // Institute admin.
        User admin = new User();
        admin.setInstituteId(instituteId);
        admin.setName("Demo Admin");
        admin.setEmail("admin@demo.test");
        admin.setPasswordHash(passwordEncoder.encode("admin12345"));
        admin.setRole(Role.INSTITUTE_ADMIN);
        userRepository.save(admin);

        // Students.
        List<User> students = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User student = new User();
            student.setInstituteId(instituteId);
            student.setName("Student " + i);
            student.setEmail("student" + i + "@demo.test");
            student.setPasswordHash(passwordEncoder.encode("student123"));
            student.setRole(Role.STUDENT);
            students.add(userRepository.save(student));
        }

        // Batch + enrollment.
        Batch batch = new Batch();
        batch.setName("Class 10 - Science");
        batch.setDescription("Demo batch for Class 10 science students.");
        batch = batchRepository.save(batch);

        for (User student : students) {
            BatchStudent enrollment = new BatchStudent();
            enrollment.setBatchId(batch.getId());
            enrollment.setStudentId(student.getId());
            batchStudentRepository.save(enrollment);
        }

        // Notes.
        Note note1 = new Note();
        note1.setBatchId(batch.getId());
        note1.setSubject("Physics");
        note1.setTitle("Laws of Motion - Summary");
        note1.setFileUrl("https://example.com/demo/laws-of-motion.pdf");
        note1.setUploadedBy(admin.getId());
        noteRepository.save(note1);

        Note note2 = new Note();
        note2.setBatchId(batch.getId());
        note2.setSubject("Chemistry");
        note2.setTitle("Periodic Table - Cheat Sheet");
        note2.setFileUrl("https://example.com/demo/periodic-table.pdf");
        note2.setUploadedBy(admin.getId());
        noteRepository.save(note2);

        // Mock test + questions.
        MockTest test = new MockTest();
        test.setBatchId(batch.getId());
        test.setTitle("Physics Mock Test 1");
        test.setDurationMinutes(30);
        test.setPublished(true);
        test = mockTestRepository.save(test);

        int totalMarks = 0;
        totalMarks += saveQuestion(test.getId(), "What is the SI unit of force?",
                "Joule", "Newton", "Pascal", "Watt", AnswerOption.B, 1);
        totalMarks += saveQuestion(test.getId(), "Acceleration is the rate of change of?",
                "Distance", "Speed", "Velocity", "Mass", AnswerOption.C, 1);
        totalMarks += saveQuestion(test.getId(), "Which law explains action and reaction?",
                "Newton's First Law", "Newton's Second Law", "Newton's Third Law", "Law of Gravitation",
                AnswerOption.C, 2);
        totalMarks += saveQuestion(test.getId(), "What is the value of g near Earth's surface (m/s^2)?",
                "8.9", "9.8", "10.8", "11.2", AnswerOption.B, 1);

        test.setTotalMarks(totalMarks);
        mockTestRepository.save(test);
    }

    private int saveQuestion(Long testId, String text, String a, String b, String c, String d,
                             AnswerOption correct, int marks) {
        Question question = new Question();
        question.setTestId(testId);
        question.setType(com.vidyapeet.exam.QuestionType.MCQ);
        question.setText(text);
        question.setOptionA(a);
        question.setOptionB(b);
        question.setOptionC(c);
        question.setOptionD(d);
        question.setCorrectAnswer(correct.name());
        question.setMarks(marks);
        questionRepository.save(question);
        return marks;
    }
}
