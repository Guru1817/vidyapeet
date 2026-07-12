package com.vidyapeet.exam;

import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * INSTITUTE_ADMIN lifecycle for {@link Question} images: validate + store on upload,
 * stream on download, and clear + best-effort delete on removal. Tenant isolation is
 * guaranteed by the {@code @Filter} on {@link Question} — a question id that belongs to
 * another institute never resolves, so its image cannot be read or written.
 */
@Service
public class QuestionImageService {

    private final QuestionRepository questionRepository;
    private final StorageService storageService;

    public QuestionImageService(QuestionRepository questionRepository, StorageService storageService) {
        this.questionRepository = questionRepository;
        this.storageService = storageService;
    }

    /** A stored image ready to stream, together with its resolved media type. */
    public record StreamableImage(Resource resource, MediaType mediaType) {
    }

    /**
     * Validates and stores the uploaded image, associating the returned storage key with
     * the question. Any previously attached image is best-effort deleted. The key is only
     * set after a successful store, so a rejected/failed upload leaves the question unchanged.
     */
    @Transactional
    public String uploadImage(Long questionId, MultipartFile file) {
        Question question = requireQuestion(questionId);
        String previousKey = question.getImageKey();
        String key = storageService.store(file, StorageService.IMAGE_CONTENT_TYPES, StorageService.IMAGE_MAX_BYTES);
        question.setImageKey(key);
        questionRepository.save(question);
        if (previousKey != null && !previousKey.equals(key)) {
            storageService.delete(previousKey);
        }
        return key;
    }

    /** Loads the question's stored image with the media type derived from its key. */
    @Transactional(readOnly = true)
    public StreamableImage getImage(Long questionId) {
        Question question = requireQuestion(questionId);
        String key = question.getImageKey();
        if (key == null) {
            throw Exceptions.notFound("This question has no image.");
        }
        Resource resource = storageService.loadAsResource(key);
        return new StreamableImage(resource, mediaTypeForKey(key));
    }

    /** Clears the question's image key and best-effort deletes the stored file. */
    @Transactional
    public void deleteImage(Long questionId) {
        Question question = requireQuestion(questionId);
        String key = question.getImageKey();
        if (key == null) {
            return;
        }
        question.setImageKey(null);
        questionRepository.save(question);
        storageService.delete(key);
    }

    private Question requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No question found with id " + id + "."));
    }

    private static MediaType mediaTypeForKey(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
