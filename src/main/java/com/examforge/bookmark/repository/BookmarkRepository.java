package com.examforge.bookmark.repository;

import com.examforge.bookmark.domain.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Bookmark> findByUserIdAndQuestionId(UUID userId, UUID questionId);

    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);
}
