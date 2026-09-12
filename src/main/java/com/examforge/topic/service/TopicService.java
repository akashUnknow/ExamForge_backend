package com.examforge.topic.service;

import com.examforge.topic.dto.TopicRequest;
import com.examforge.topic.dto.TopicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TopicService {

    /** Public: only valid if the grandparent exam is PUBLISHED. */
    Page<TopicResponse> getPublicBySubject(UUID subjectId, Pageable pageable);

    /** Admin: grandparent exam can be in any status. */
    Page<TopicResponse> getAdminBySubject(UUID subjectId, Pageable pageable);

    TopicResponse getAdminById(UUID id);

    TopicResponse create(TopicRequest request);

    TopicResponse update(UUID id, TopicRequest request);

    void delete(UUID id);
}
