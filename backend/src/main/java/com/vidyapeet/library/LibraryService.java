package com.vidyapeet.library;

import com.vidyapeet.batch.BatchStudent;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.BatchTest;
import com.vidyapeet.exam.ExamService;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.dto.TestResponse;
import com.vidyapeet.exam.repository.BatchTestRepository;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.library.dto.FolderDetailResponse;
import com.vidyapeet.library.dto.FolderRequest;
import com.vidyapeet.library.dto.FolderResponse;
import com.vidyapeet.library.dto.LibraryFileResponse;
import com.vidyapeet.library.repository.BatchLibraryFileRepository;
import com.vidyapeet.library.repository.LibraryFileRepository;
import com.vidyapeet.library.repository.LibraryFolderRepository;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.security.UserPrincipal;
import com.vidyapeet.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * INSTITUTE_ADMIN management of the content library: folders, PDF files, and
 * sharing library files/tests with batches.
 */
@Service
public class LibraryService {

    private final LibraryFolderRepository folderRepository;
    private final LibraryFileRepository fileRepository;
    private final BatchLibraryFileRepository batchFileRepository;
    private final BatchTestRepository batchTestRepository;
    private final MockTestRepository testRepository;
    private final BatchRepository batchRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final StorageService storageService;
    private final ExamService examService;

    public LibraryService(
            LibraryFolderRepository folderRepository,
            LibraryFileRepository fileRepository,
            BatchLibraryFileRepository batchFileRepository,
            BatchTestRepository batchTestRepository,
            MockTestRepository testRepository,
            BatchRepository batchRepository,
            BatchStudentRepository batchStudentRepository,
            StorageService storageService,
            ExamService examService) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.batchFileRepository = batchFileRepository;
        this.batchTestRepository = batchTestRepository;
        this.testRepository = testRepository;
        this.batchRepository = batchRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.storageService = storageService;
        this.examService = examService;
    }

    public record DownloadableFile(Resource resource, String filename) {
    }

    // --- folders ---

    @Transactional
    public FolderResponse createFolder(FolderRequest request) {
        LibraryFolder folder = new LibraryFolder();
        folder.setName(request.name());
        folder.setDescription(request.description());
        folder = folderRepository.save(folder);
        return FolderResponse.from(folder, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listFolders() {
        return folderRepository.findAllByOrderByNameAsc().stream()
                .map(f -> FolderResponse.from(f,
                        fileRepository.countByFolderId(f.getId()),
                        testRepository.countByFolderId(f.getId())))
                .toList();
    }

    @Transactional
    public FolderResponse updateFolder(Long id, FolderRequest request) {
        LibraryFolder folder = requireFolder(id);
        folder.setName(request.name());
        folder.setDescription(request.description());
        folder = folderRepository.save(folder);
        return FolderResponse.from(folder,
                fileRepository.countByFolderId(id), testRepository.countByFolderId(id));
    }

    @Transactional
    public void deleteFolder(Long id) {
        requireFolder(id);
        // Remove files (and their batch assignments + stored blobs).
        List<LibraryFile> files = fileRepository.findByFolderIdOrderByCreatedAtDesc(id);
        for (LibraryFile f : files) {
            batchFileRepository.deleteByLibraryFileId(f.getId());
            storageService.delete(f.getFileUrl());
        }
        fileRepository.deleteByFolderId(id);
        // Remove library tests in the folder (cascades attempts/questions/assignments).
        for (MockTest t : testRepository.findByFolderIdOrderByCreatedAtDesc(id)) {
            examService.deleteTest(t.getId());
        }
        folderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FolderDetailResponse getFolderDetail(Long id) {
        LibraryFolder folder = requireFolder(id);
        List<LibraryFileResponse> files = fileRepository.findByFolderIdOrderByCreatedAtDesc(id).stream()
                .map(LibraryFileResponse::from)
                .toList();
        List<TestResponse> tests = examService.listLibraryTests(id);
        return new FolderDetailResponse(folder.getId(), folder.getName(), folder.getDescription(), files, tests);
    }

    // --- files ---

    @Transactional
    public LibraryFileResponse uploadFile(Long folderId, String subject, String title, MultipartFile file) {
        requireFolder(folderId);
        String key = storageService.store(file);
        LibraryFile lf = new LibraryFile();
        lf.setFolderId(folderId);
        lf.setSubject(subject);
        lf.setTitle(title);
        lf.setFileUrl(key);
        lf.setFileSize(file.getSize());
        lf.setUploadedBy(SecurityUtils.currentUserId());
        return LibraryFileResponse.from(fileRepository.save(lf));
    }

    @Transactional
    public void deleteFile(Long fileId) {
        LibraryFile file = requireFile(fileId);
        batchFileRepository.deleteByLibraryFileId(fileId);
        fileRepository.delete(file);
        storageService.delete(file.getFileUrl());
    }

    @Transactional(readOnly = true)
    public DownloadableFile download(Long fileId) {
        LibraryFile file = requireFile(fileId);
        authorizeDownload(file);
        Resource resource = storageService.loadAsResource(file.getFileUrl());
        String safeTitle = file.getTitle().replaceAll("[^a-zA-Z0-9-_ ]", "_").trim();
        return new DownloadableFile(resource, safeTitle + ".pdf");
    }

    // --- batch assignment ---

    @Transactional
    public void assignFileToBatch(Long batchId, Long fileId) {
        requireBatch(batchId);
        requireFile(fileId);
        if (batchFileRepository.existsByBatchIdAndLibraryFileId(batchId, fileId)) {
            return;
        }
        BatchLibraryFile link = new BatchLibraryFile();
        link.setBatchId(batchId);
        link.setLibraryFileId(fileId);
        batchFileRepository.save(link);
    }

    @Transactional
    public void unassignFileFromBatch(Long batchId, Long fileId) {
        BatchLibraryFile link = batchFileRepository.findByBatchIdAndLibraryFileId(batchId, fileId)
                .orElseThrow(() -> Exceptions.notFound("This file is not assigned to the batch."));
        batchFileRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public List<LibraryFileResponse> listBatchFiles(Long batchId) {
        requireBatch(batchId);
        List<Long> fileIds = batchFileRepository.findByBatchId(batchId).stream()
                .map(BatchLibraryFile::getLibraryFileId).toList();
        if (fileIds.isEmpty()) {
            return List.of();
        }
        return fileRepository.findAllById(fileIds).stream().map(LibraryFileResponse::from).toList();
    }

    @Transactional
    public void assignTestToBatch(Long batchId, Long testId) {
        requireBatch(batchId);
        requireTest(testId);
        if (batchTestRepository.existsByBatchIdAndTestId(batchId, testId)) {
            return;
        }
        BatchTest link = new BatchTest();
        link.setBatchId(batchId);
        link.setTestId(testId);
        batchTestRepository.save(link);
    }

    @Transactional
    public void unassignTestFromBatch(Long batchId, Long testId) {
        BatchTest link = batchTestRepository.findByBatchIdAndTestId(batchId, testId)
                .orElseThrow(() -> Exceptions.notFound("This test is not assigned to the batch."));
        batchTestRepository.delete(link);
    }

    // --- helpers ---

    private void authorizeDownload(LibraryFile file) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == Role.INSTITUTE_ADMIN) {
            return;
        }
        if (principal.getRole() == Role.STUDENT) {
            List<Long> batchIds = batchStudentRepository.findByStudentId(principal.getUserId()).stream()
                    .map(BatchStudent::getBatchId).toList();
            for (Long b : batchIds) {
                if (batchFileRepository.existsByBatchIdAndLibraryFileId(b, file.getId())) {
                    return;
                }
            }
        }
        throw Exceptions.forbidden("You do not have access to this file.");
    }

    private LibraryFolder requireFolder(Long id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No folder found with id " + id + "."));
    }

    private LibraryFile requireFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No file found with id " + id + "."));
    }

    private void requireBatch(Long id) {
        if (batchRepository.findById(id).isEmpty()) {
            throw Exceptions.notFound("No batch found with id " + id + ".");
        }
    }

    private void requireTest(Long id) {
        if (testRepository.findById(id).isEmpty()) {
            throw Exceptions.notFound("No test found with id " + id + ".");
        }
    }
}
