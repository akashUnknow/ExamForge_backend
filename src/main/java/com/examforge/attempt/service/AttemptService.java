package com.examforge.attempt.service;

import com.examforge.attempt.dto.AnswerRequest;
import com.examforge.attempt.dto.AttemptResponse;
import com.examforge.attempt.dto.AttemptResultResponse;
import com.examforge.attempt.dto.QuestionAttemptResponse;

import java.util.List;
import java.util.UUID;

public interface AttemptService {

    /** Starts a new attempt, or resumes an existing IN_PROGRESS one for this user+test. */
    AttemptResponse startAttempt(UUID testId);

    /** Answer-free question list for an in-progress attempt, in test order. */
    List<QuestionAttemptResponse> getQuestions(UUID attemptId);

    void saveAnswer(UUID attemptId, AnswerRequest request);

    AttemptResultResponse submit(UUID attemptId);

    /** Only available once the attempt is SUBMITTED or AUTO_SUBMITTED. */
    AttemptResultResponse getResult(UUID attemptId);

    /**
     * Finds every IN_PROGRESS attempt whose deadline has already passed and
     * finalizes each as AUTO_SUBMITTED. Called by the scheduled sweep job -
     * the lazy per-request check in the other methods only catches an
     * expired attempt when someone touches it again; this catches the ones
     * nobody ever comes back to. Returns the number of attempts finalized.
     */
    int autoSubmitAllExpired();
}
