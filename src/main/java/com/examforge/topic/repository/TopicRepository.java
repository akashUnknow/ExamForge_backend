package com.examforge.topic.repository;

import com.examforge.topic.domain.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Page<Topic> findBySubjectIdOrderByDisplayOrderAsc(UUID subjectId, Pageable pageable);

    boolean existsBySubjectId(UUID subjectId);

    boolean existsBySubjectIdAndNameIgnoreCase(UUID subjectId, String name);
}
