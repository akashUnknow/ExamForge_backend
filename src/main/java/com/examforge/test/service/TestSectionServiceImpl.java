package com.examforge.test.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestSection;
import com.examforge.test.dto.TestSectionRequest;
import com.examforge.test.dto.TestSectionResponse;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestQuestionRepository;
import com.examforge.test.repository.TestSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestSectionServiceImpl implements TestSectionService {

    private final TestSectionRepository sectionRepository;
    private final TestPaperRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TestSectionResponse> getByTest(UUID testId) {
        if (!testRepository.existsById(testId)) {
            throw ResourceNotFoundException.of("Test", testId);
        }
        return sectionRepository.findByTestIdOrderByDisplayOrderAsc(testId).stream()
                .map(TestSectionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestSectionResponse create(UUID testId, TestSectionRequest request) {
        TestPaper test = testRepository.findById(testId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        String name = request.getName().trim();
        if (sectionRepository.existsByTestIdAndNameIgnoreCase(testId, name)) {
            throw new DuplicateResourceException("A section named '" + name + "' already exists for this test");
        }

        TestSection section = new TestSection();
        section.setTest(test);
        applyRequest(section, request, name);

        return TestSectionResponse.from(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public TestSectionResponse update(UUID testId, UUID sectionId, TestSectionRequest request) {
        TestSection section = findEntity(testId, sectionId);

        String name = request.getName().trim();
        if (!name.equalsIgnoreCase(section.getName()) && sectionRepository.existsByTestIdAndNameIgnoreCase(testId, name)) {
            throw new DuplicateResourceException("A section named '" + name + "' already exists for this test");
        }

        applyRequest(section, request, name);
        return TestSectionResponse.from(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public void delete(UUID testId, UUID sectionId) {
        TestSection section = findEntity(testId, sectionId);

        if (testQuestionRepository.existsBySectionId(sectionId)) {
            throw new ResourceInUseException("Cannot delete this section while questions are still assigned to it");
        }

        section.setDeletedAt(Instant.now());
        sectionRepository.save(section);
    }

    private void applyRequest(TestSection section, TestSectionRequest request, String name) {
        section.setName(name);
        section.setDurationMinutes(request.getDurationMinutes());
        section.setQuestionCount(request.getQuestionCount());
        section.setMarks(request.getMarks());
        section.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
    }

    private TestSection findEntity(UUID testId, UUID sectionId) {
        TestSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test section", sectionId));
        if (!section.getTest().getId().equals(testId)) {
            throw ResourceNotFoundException.of("Test section", sectionId);
        }
        return section;
    }
}
