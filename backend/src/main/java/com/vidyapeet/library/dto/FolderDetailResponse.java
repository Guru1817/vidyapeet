package com.vidyapeet.library.dto;

import com.vidyapeet.exam.dto.TestResponse;

import java.util.List;

public record FolderDetailResponse(
        Long id,
        String name,
        String description,
        List<LibraryFileResponse> files,
        List<TestResponse> tests
) {
}
