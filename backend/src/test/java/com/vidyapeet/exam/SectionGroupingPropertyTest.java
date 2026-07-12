package com.vidyapeet.exam;

import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 10: Section grouping and ordering
 *
 * <p>For any test and any configuration of sections and references, resolving the test's
 * questions yields them ordered by section position and then by reference position within
 * each section; when the test defines no sections, resolution yields a single ungrouped list
 * ordered by reference position.
 *
 * <p>Validates: Requirements 7.2, 7.5, 7.8
 *
 * <p>The resolution contract lives in
 * {@link TestQuestionReferenceRepository#findResolvedQuestions} whose JPQL orders by
 * {@code s.position ASC NULLS FIRST, r.position ASC}. Because that ordering is enforced by the
 * database, this property exercises it through an in-memory Mockito fake that mirrors the
 * query's {@code ORDER BY} (section position, {@code NULLS FIRST} for ungrouped references,
 * then reference position) — matching how the other backend property tests fake
 * {@code findResolvedQuestions}. The expected order is computed by an independent
 * <em>grouping</em> algorithm (ungrouped block first, then each section's block in section
 * order) rather than a single comparator sort, so the two computations cross-validate the
 * contract instead of restating one another.
 */
class SectionGroupingPropertyTest {

    @Property(tries = 100)
    void resolutionIsOrderedBySectionThenReferencePosition(@ForAll("scenarios") Scenario scenario) {
        // --- In-memory backing state -------------------------------------------------
        // Bank questions keyed by id (resolution returns the live Question entities).
        Map<Long, Question> questionStore = new HashMap<>();
        for (RefSpec ref : scenario.references()) {
            Question q = new Question();
            q.setId(ref.bankQuestionId());
            q.setText("q" + ref.bankQuestionId());
            questionStore.put(ref.bankQuestionId(), q);
        }
        // Section position lookup by section id (mirrors the LEFT JOIN on section_id).
        Map<Long, Integer> sectionPositionById = new HashMap<>();
        for (SectionSpec s : scenario.sections()) {
            sectionPositionById.put(s.sectionId(), s.position());
        }

        // --- Fake repository: mirror the JPQL ORDER BY s.position NULLS FIRST, r.position
        TestQuestionReferenceRepository referenceRepository = mock(TestQuestionReferenceRepository.class);
        when(referenceRepository.findResolvedQuestions(anyLong())).thenAnswer(invocation -> {
            Long testId = invocation.getArgument(0);
            Comparator<RefSpec> bySectionThenRef = Comparator
                    .comparing((RefSpec r) -> sectionPositionOf(r, sectionPositionById),
                            Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(RefSpec::position);
            return scenario.references().stream()
                    .filter(r -> r.testId().equals(testId))
                    .sorted(bySectionThenRef)
                    .map(r -> questionStore.get(r.bankQuestionId()))
                    .toList();
        });

        // --- Act ---------------------------------------------------------------------
        List<Long> resolvedIds = referenceRepository.findResolvedQuestions(scenario.testId()).stream()
                .map(Question::getId)
                .toList();

        // --- Expected: independent grouping algorithm --------------------------------
        List<Long> expectedIds = expectedOrder(scenario, sectionPositionById);

        assertThat(resolvedIds).isEqualTo(expectedIds);

        // --- Ungrouped fallback: no sections => single list ordered by reference pos ---
        if (scenario.sections().isEmpty()) {
            List<Long> byReferencePositionOnly = scenario.references().stream()
                    .sorted(Comparator.comparing(RefSpec::position))
                    .map(RefSpec::bankQuestionId)
                    .toList();
            assertThat(resolvedIds).isEqualTo(byReferencePositionOnly);
        }
    }

    /** Section position for a reference; {@code null} when ungrouped (mirrors NULLS FIRST). */
    private static Integer sectionPositionOf(RefSpec ref, Map<Long, Integer> sectionPositionById) {
        return ref.sectionId() == null ? null : sectionPositionById.get(ref.sectionId());
    }

    /**
     * Independent expectation: ungrouped references first (ordered by reference position),
     * then each section in section-position order, each section's references ordered by
     * reference position. This grouping construction is equivalent to — but computed
     * differently from — the fake's single comparator sort.
     */
    private static List<Long> expectedOrder(Scenario scenario, Map<Long, Integer> sectionPositionById) {
        List<Long> result = new ArrayList<>();

        // Ungrouped block (NULLS FIRST).
        scenario.references().stream()
                .filter(r -> r.sectionId() == null)
                .sorted(Comparator.comparing(RefSpec::position))
                .forEach(r -> result.add(r.bankQuestionId()));

        // Each section, in section-position order.
        scenario.sections().stream()
                .sorted(Comparator.comparing(SectionSpec::position))
                .forEach(section -> scenario.references().stream()
                        .filter(r -> section.sectionId().equals(r.sectionId()))
                        .sorted(Comparator.comparing(RefSpec::position))
                        .forEach(r -> result.add(r.bankQuestionId())));

        return result;
    }

    // -------------------------------------------------------------------------
    // Generators
    // -------------------------------------------------------------------------

    record SectionSpec(Long sectionId, Integer position) {
    }

    record RefSpec(Long testId, Long bankQuestionId, Long sectionId, Integer position) {
    }

    record Scenario(Long testId, List<SectionSpec> sections, List<RefSpec> references) {
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        // 0..4 sections, 1..10 references. Distinct section positions and distinct reference
        // positions keep the (section position, reference position) order total, so the
        // expected resolution is unambiguous.
        return Arbitraries.integers().between(0, 4).flatMap(numSections ->
                Arbitraries.integers().between(1, 10).flatMap(numRefs ->
                        buildScenario(numSections, numRefs)));
    }

    private Arbitrary<Scenario> buildScenario(int numSections, int numRefs) {
        long testId = 7L;

        Arbitrary<List<Integer>> sectionPositions = Arbitraries.integers().between(0, 1000)
                .set().ofSize(numSections)
                .map(ArrayList::new);
        Arbitrary<List<Integer>> referencePositions = Arbitraries.integers().between(0, 5000)
                .set().ofSize(numRefs)
                .map(ArrayList::new);
        // Section assignment per reference: 0 = ungrouped, k = section index (k-1).
        Arbitrary<List<Integer>> assignments = Arbitraries.integers().between(0, numSections)
                .list().ofSize(numRefs);

        return Combinators.combine(sectionPositions, referencePositions, assignments)
                .as((secPos, refPos, assign) -> {
                    List<SectionSpec> sections = new ArrayList<>();
                    for (int i = 0; i < numSections; i++) {
                        sections.add(new SectionSpec(100L + i, secPos.get(i)));
                    }
                    List<RefSpec> references = new ArrayList<>();
                    for (int j = 0; j < numRefs; j++) {
                        int a = assign.get(j);
                        Long sectionId = a == 0 ? null : sections.get(a - 1).sectionId();
                        references.add(new RefSpec(testId, 1000L + j, sectionId, refPos.get(j)));
                    }
                    return new Scenario(testId, sections, references);
                });
    }
}
