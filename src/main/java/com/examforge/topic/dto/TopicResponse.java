package com.examforge.topic.dto;

import com.examforge.topic.domain.Topic;

import java.time.Instant;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        UUID subjectId,
        String name,
        String description,
        Integer displayOrder,
        Instant createdAt
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getSubject().getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getDisplayOrder(),
                topic.getCreatedAt()
        );
    }
}
