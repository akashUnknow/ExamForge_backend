package com.examforge.ranking.service;

import com.examforge.attempt.domain.TestAttempt;
import com.examforge.attempt.repository.TestAttemptRepository;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.response.PageResponse;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.ranking.dto.RankingResponse;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.repository.TestPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final TestAttemptRepository attemptRepository;
    private final TestPaperRepository testRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RankingResponse> getRanking(UUID testId, Pageable pageable) {
        testRepository.findById(testId)
                .filter(t -> t.getStatus() == TestStatus.PUBLISHED && t.getExam().getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        Page<TestAttempt> bestPerUser = attemptRepository.findBestAttemptPerUser(testId, pageable);

        int startRank = (int) pageable.getOffset() + 1;
        List<RankingResponse> content = new ArrayList<>();
        int i = 0;
        for (TestAttempt attempt : bestPerUser.getContent()) {
            content.add(new RankingResponse(
                    startRank + i,
                    attempt.getUser().getId(),
                    attempt.getUser().getName(),
                    attempt.getScore(),
                    attempt.getAccuracy()
            ));
            i++;
        }

        return new PageResponse<>(
                content,
                bestPerUser.getNumber(),
                bestPerUser.getSize(),
                bestPerUser.getTotalElements(),
                bestPerUser.getTotalPages(),
                bestPerUser.isFirst(),
                bestPerUser.isLast()
        );
    }
}
