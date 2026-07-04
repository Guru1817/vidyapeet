package com.vidyapeet.batch.dto;

import com.vidyapeet.batch.Batch;

public record BatchResponse(
        Long id,
        String name,
        String description,
        long studentCount
) {
    public static BatchResponse from(Batch batch, long studentCount) {
        return new BatchResponse(batch.getId(), batch.getName(), batch.getDescription(), studentCount);
    }
}
