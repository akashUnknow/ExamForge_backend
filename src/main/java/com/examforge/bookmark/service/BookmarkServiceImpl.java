package com.examforge.bookmark.service;

import com.examforge.bookmark.domain.Bookmark;
import com.examforge.bookmark.dto.BookmarkResponse;
import com.examforge.bookmark.repository.BookmarkRepository;
import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.question.domain.Question;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookmarkResponse> getMyBookmarks(Pageable pageable) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(currentUserId(), pageable)
                .map(BookmarkResponse::from);
    }

    @Override
    @Transactional
    public BookmarkResponse addBookmark(UUID questionId) {
        UUID userId = currentUserId();

        if (bookmarkRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            throw new DuplicateResourceException("This question is already bookmarked");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Question", questionId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setQuestion(question);

        return BookmarkResponse.from(bookmarkRepository.save(bookmark));
    }

    @Override
    @Transactional
    public void removeBookmark(UUID questionId) {
        UUID userId = currentUserId();
        Bookmark bookmark = bookmarkRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Bookmark", questionId));
        bookmarkRepository.delete(bookmark);
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ForbiddenException("Authentication is required"));
    }
}
