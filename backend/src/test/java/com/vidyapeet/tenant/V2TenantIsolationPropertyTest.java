package com.vidyapeet.tenant;

import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionType;
import com.vidyapeet.exam.TestQuestionReference;
import com.vidyapeet.exam.TestSection;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import com.vidyapeet.institute.Institute;
import com.vidyapeet.institute.repository.InstituteRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 12: Tenant isolation across all new V2 tables
 *
 * <p>For any pair of distinct institutes, neither can observe the other's rows in any of the
 * new V2 tables — bank {@code Question}s (including their {@code image_key}),
 * {@code TestQuestionReference}s, and {@code TestSection}s — through repository queries
 * ({@code findAll}, the {@code findByTestId...} queries, {@code findResolvedQuestions},
 * {@code findByBankQuestionId}) nor through {@code findById}.
 *
 * <p>Validates: Requirements 5.8, 6.1, 6.6, 7.9, 8.1
 *
 * <p>This property must exercise the real Hibernate {@code tenantFilter} enabled per session by
 * {@link TenantFilterAspect}, so it runs inside a live Spring context following the pattern of
 * {@link TenantIsolationTest} (seed/query as a tenant via {@link TenantContext}, run reads inside a
 * {@link TransactionTemplate} so the filter is active on the transactional session, and clean the
 * shared H2 instance under {@code bypass}). jqwik does not participate in the JUnit Jupiter
 * extension model, so rather than a {@code @Property} (whose {@code @Autowired} fields would not be
 * injected per iteration) this drives 100+ jqwik-generated scenarios inside a single
 * {@code @SpringBootTest} {@code @Test} via {@link Arbitrary#sampleStream()}. Each generated
 * scenario reuses the same booted context and the same real filter, and the per-scenario check is
 * the property assertion.
 */
@SpringBootTest(properties = "vidyapeet.seed.enabled=false")
@ActiveProfiles("dev")
class V2TenantIsolationPropertyTest {

    /** Both tenants deliberately share this test id so isolation is proven by the filter alone. */
    private static final long SHARED_TEST_ID = 7777L;

    private static final int ITERATIONS = 100;

    @Autowired
    private InstituteRepository instituteRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestQuestionReferenceRepository referenceRepository;

    @Autowired
    private TestSectionRepository sectionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        cleanAll();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
        TenantContext.clear();
    }

    @Test
    void tenantIsolationHoldsAcrossAllNewV2Tables() {
        // 100+ generated tenant configurations exercised against the real tenant @Filter.
        scenarios().sampleStream().limit(ITERATIONS).forEach(this::checkScenario);
    }

    private Arbitrary<Scenario> scenarios() {
        Arbitrary<Integer> questionCounts = Arbitraries.integers().between(1, 3);
        Arbitrary<Integer> sectionCounts = Arbitraries.integers().between(0, 2);
        return Combinators.combine(questionCounts, sectionCounts, questionCounts, sectionCounts)
                .as(Scenario::new);
    }

    private void checkScenario(Scenario scenario) {
        cleanAll();

        Long tenantA = createInstitute("Tenant A", "tenant-a");
        Long tenantB = createInstitute("Tenant B", "tenant-b");

        Seeded seededA = seedTenant(tenantA, scenario.questionsA(), scenario.sectionsA(), "a");
        Seeded seededB = seedTenant(tenantB, scenario.questionsB(), scenario.sectionsB(), "b");

        // Each tenant sees exactly its own rows and none of the other tenant's rows.
        verifyIsolation(tenantA, seededA, seededB);
        verifyIsolation(tenantB, seededB, seededA);
    }

    /**
     * Asserts the {@code viewer} tenant sees only its {@code own} rows across every V2 table and
     * cannot reach any of the {@code other} tenant's rows via collection queries or {@code findById}.
     */
    private void verifyIsolation(Long viewer, Seeded own, Seeded other) {
        TenantContext.setTenantId(viewer);
        try {
            tx.executeWithoutResult(status -> {
                // --- Bank questions (incl. image_key) --------------------------------------
                List<Question> visibleQuestions = questionRepository.findAll();
                assertThat(visibleQuestions).allMatch(q -> q.getInstituteId().equals(viewer));
                assertThat(visibleQuestions).extracting(Question::getId)
                        .containsExactlyInAnyOrderElementsOf(own.questionIds());

                for (Long ownId : own.questionIds()) {
                    Optional<Question> found = questionRepository.findById(ownId);
                    assertThat(found).isPresent();
                    // The attached image is reachable only to the owning tenant.
                    assertThat(found.get().getImageKey()).isNotNull();
                }
                for (Long otherId : other.questionIds()) {
                    assertThat(questionRepository.findById(otherId)).isEmpty();
                }

                // --- Test question references ----------------------------------------------
                List<TestQuestionReference> visibleRefs = referenceRepository.findAll();
                assertThat(visibleRefs).allMatch(r -> r.getInstituteId().equals(viewer));
                assertThat(visibleRefs).extracting(TestQuestionReference::getId)
                        .containsExactlyInAnyOrderElementsOf(own.referenceIds());

                // Both tenants share SHARED_TEST_ID, so scoped-by-test queries must still isolate.
                assertThat(referenceRepository.findByTestIdOrderBySectionPositionAscPositionAsc(SHARED_TEST_ID))
                        .extracting(TestQuestionReference::getId)
                        .containsExactlyInAnyOrderElementsOf(own.referenceIds());
                assertThat(referenceRepository.findResolvedQuestions(SHARED_TEST_ID))
                        .extracting(Question::getId)
                        .containsExactlyInAnyOrderElementsOf(own.questionIds());

                for (Long otherRefId : other.referenceIds()) {
                    assertThat(referenceRepository.findById(otherRefId)).isEmpty();
                }
                for (Long otherQuestionId : other.questionIds()) {
                    assertThat(referenceRepository.findByBankQuestionId(otherQuestionId)).isEmpty();
                }

                // --- Test sections ----------------------------------------------------------
                List<TestSection> visibleSections = sectionRepository.findAll();
                assertThat(visibleSections).allMatch(s -> s.getInstituteId().equals(viewer));
                assertThat(visibleSections).extracting(TestSection::getId)
                        .containsExactlyInAnyOrderElementsOf(own.sectionIds());

                assertThat(sectionRepository.findByTestIdOrderByPositionAsc(SHARED_TEST_ID))
                        .extracting(TestSection::getId)
                        .containsExactlyInAnyOrderElementsOf(own.sectionIds());

                for (Long otherSectionId : other.sectionIds()) {
                    assertThat(sectionRepository.findById(otherSectionId)).isEmpty();
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    private Seeded seedTenant(Long instituteId, int questionCount, int sectionCount, String prefix) {
        TenantContext.setTenantId(instituteId);
        try {
            List<Long> sectionIds = new ArrayList<>();
            for (int i = 0; i < sectionCount; i++) {
                TestSection section = new TestSection();
                section.setTestId(SHARED_TEST_ID);
                section.setLabel(prefix + "-section-" + i);
                section.setPosition(i);
                sectionIds.add(sectionRepository.save(section).getId());
            }

            List<Long> questionIds = new ArrayList<>();
            List<Long> referenceIds = new ArrayList<>();
            for (int i = 0; i < questionCount; i++) {
                Question question = new Question();
                question.setType(QuestionType.MCQ);
                question.setText(prefix + " question " + i);
                question.setOptionA("A");
                question.setOptionB("B");
                question.setOptionC("C");
                question.setOptionD("D");
                question.setCorrectAnswer("A");
                question.setMarks(1);
                // Every bank question carries an image_key so image isolation is exercised too.
                question.setImageKey(prefix + "-image-" + i + ".png");
                Long questionId = questionRepository.save(question).getId();
                questionIds.add(questionId);

                TestQuestionReference reference = new TestQuestionReference();
                reference.setTestId(SHARED_TEST_ID);
                reference.setBankQuestionId(questionId);
                reference.setSectionId(sectionIds.isEmpty() ? null : sectionIds.get(i % sectionIds.size()));
                reference.setPosition(i);
                referenceIds.add(referenceRepository.save(reference).getId());
            }

            return new Seeded(questionIds, referenceIds, sectionIds);
        } finally {
            TenantContext.clear();
        }
    }

    private Long createInstitute(String name, String slug) {
        Institute institute = new Institute();
        institute.setName(name);
        institute.setSlug(slug);
        institute.setPrimaryColor("#000000");
        return instituteRepository.save(institute).getId();
    }

    /** The H2 instance is shared across tests and iterations (no rollback); start each clean. */
    private void cleanAll() {
        TenantContext.setBypass(true);
        try {
            referenceRepository.deleteAll();
            sectionRepository.deleteAll();
            questionRepository.deleteAll();
            instituteRepository.deleteAll();
        } finally {
            TenantContext.clear();
        }
    }

    private record Scenario(int questionsA, int sectionsA, int questionsB, int sectionsB) {
    }

    private record Seeded(List<Long> questionIds, List<Long> referenceIds, List<Long> sectionIds) {
    }
}
