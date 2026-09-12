package com.examforge.note.service;

import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.note.domain.UserNote;
import com.examforge.note.dto.NoteResponse;
import com.examforge.note.repository.UserNoteRepository;
import com.examforge.question.domain.Question;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final UserNoteRepository noteRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<NoteResponse> getMyNote(UUID questionId) {
        return noteRepository.findByUserIdAndQuestionId(currentUserId(), questionId).map(NoteResponse::from);
    }

    @Override
    @Transactional
    public NoteResponse saveMyNote(UUID questionId, String noteText) {
        UUID userId = currentUserId();

        UserNote note = noteRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> {
                    Question question = questionRepository.findById(questionId)
                            .orElseThrow(() -> ResourceNotFoundException.of("Question", questionId));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
                    UserNote n = new UserNote();
                    n.setUser(user);
                    n.setQuestion(question);
                    return n;
                });

        note.setNoteText(noteText);
        return NoteResponse.from(noteRepository.save(note));
    }

    @Override
    @Transactional
    public void deleteMyNote(UUID questionId) {
        UserNote note = noteRepository.findByUserIdAndQuestionId(currentUserId(), questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Note", questionId));
        noteRepository.delete(note);
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ForbiddenException("Authentication is required"));
    }
}
