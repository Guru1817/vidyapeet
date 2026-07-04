package com.vidyapeet.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequest(
        @NotBlank(message = "Folder name is required")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {
}
