package com.vidyapeet.exam;

import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.storage.LocalStorageService;
import com.vidyapeet.storage.StorageService;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 5: Question image association round-trip
 *
 * <p>For any bank question and any valid image upload, after upload the question's
 * {@code image_key} equals the storage key returned by {@link StorageService}, and that
 * key streams back the stored image.
 *
 * <p>Validates: Requirements 5.2
 *
 * <p>The round-trip goes through {@link QuestionImageService#uploadImage} then
 * {@link QuestionImageService#getImage}, backed by a real {@link LocalStorageService}
 * (per-property temp directory) and a Mockito {@link QuestionRepository} whose
 * {@code findById} returns a live {@link Question} and whose {@code save} records it — so
 * the mutation performed by upload is visible to the subsequent download. No Supabase is
 * touched across the 100+ iterations.
 */
class QuestionImageAssociationPropertyTest {

    private LocalStorageService storageService;

    @BeforeProperty
    void setUp() throws IOException {
        Path tempDir = Files.createTempDirectory("vidyapeet-question-image-pbt");
        tempDir.toFile().deleteOnExit();
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Property(tries = 100)
    void imageAssociationRoundTrips(@ForAll("imageUploads") ImageUpload upload) throws IOException {
        // A single live question the mock repository resolves by id; upload mutates it and
        // save simply records the mutation, so getImage observes the associated key.
        Question question = new Question();
        question.setId(upload.questionId());

        QuestionRepository questionRepository = mock(QuestionRepository.class);
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionImageService service = new QuestionImageService(questionRepository, storageService);

        MockMultipartFile file = new MockMultipartFile(
                "file", "diagram" + upload.extension(), upload.contentType(), upload.content());

        // Act: upload then download.
        String returnedKey = service.uploadImage(upload.questionId(), file);

        // The association: the question now carries exactly the returned storage key.
        assertThat(returnedKey).isNotBlank();
        assertThat(question.getImageKey()).isEqualTo(returnedKey);

        // That key streams back the exact bytes that were uploaded.
        QuestionImageService.StreamableImage image = service.getImage(upload.questionId());
        Resource resource = image.resource();
        assertThat(resource.exists()).isTrue();
        byte[] streamedBack;
        try (InputStream in = resource.getInputStream()) {
            streamedBack = in.readAllBytes();
        }
        assertThat(streamedBack).isEqualTo(upload.content());
    }

    /**
     * Valid image uploads: one of the allowed image content types, a non-empty payload
     * within the per-image cap, and an arbitrary question id.
     */
    @Provide
    Arbitrary<ImageUpload> imageUploads() {
        Arbitrary<String> contentTypes = Arbitraries.of("image/png", "image/jpeg", "image/webp");
        // Small non-empty payloads keep allocation cheap while exercising real byte round-trips.
        Arbitrary<byte[]> contents = Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(2048);
        Arbitrary<Long> questionIds = Arbitraries.longs().between(1L, 100_000L);

        return Combinators.combine(questionIds, contentTypes, contents)
                .as(ImageUpload::new);
    }

    /** A generated valid image upload for a given question id. */
    record ImageUpload(Long questionId, String contentType, byte[] content) {
        String extension() {
            return StorageService.extensionForContentType(contentType);
        }
    }
}
