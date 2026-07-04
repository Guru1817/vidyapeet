package com.vidyapeet.library.dto;

import com.vidyapeet.library.LibraryFolder;

public record FolderResponse(
        Long id,
        String name,
        String description,
        long fileCount,
        long testCount
) {
    public static FolderResponse from(LibraryFolder folder, long fileCount, long testCount) {
        return new FolderResponse(folder.getId(), folder.getName(), folder.getDescription(), fileCount, testCount);
    }
}
