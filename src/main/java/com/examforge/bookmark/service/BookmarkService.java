package com.examforge.bookmark.service;

import com.examforge.bookmark.dto.BookmarkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookmarkService {

    Page<BookmarkResponse> getMyBookmarks(Pageable pageable);

    BookmarkResponse addBookmark(UUID questionId);

    void removeBookmark(UUID questionId);
}
