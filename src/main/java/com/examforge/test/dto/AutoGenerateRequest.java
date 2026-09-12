package com.examforge.test.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class AutoGenerateRequest {

    @NotEmpty(message = "At least one bucket is required")
    @Valid
    private List<AutoGenerateBucket> buckets;

    /** Optional - if set, all generated questions are assigned to this section. */
    private UUID sectionId;

    public List<AutoGenerateBucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<AutoGenerateBucket> buckets) {
        this.buckets = buckets;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }
}
