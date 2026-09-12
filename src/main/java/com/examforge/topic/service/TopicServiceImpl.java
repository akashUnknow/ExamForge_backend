package com.examforge.topic.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.subject.domain.Subject;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.topic.domain.Topic;
import com.examforge.topic.dto.TopicRequest;
import com.examforge.topic.dto.TopicResponse;
import com.examforge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TopicResponse> getPublicBySubject(UUID subjectId, Pageable pageable) {
        Subject subject = subjectRepository.findById(subjectId)
                .filter(s -> s.getExam().getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", subjectId));

        return topicRepository.findBySubjectIdOrderByDisplayOrderAsc(subject.getId(), pageable)
                .map(TopicResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopicResponse> getAdminBySubject(UUID subjectId, Pageable pageable) {
        if (!subjectRepository.existsById(subjectId)) {
            throw ResourceNotFoundException.of("Subject", subjectId);
        }
        return topicRepository.findBySubjectIdOrderByDisplayOrderAsc(subjectId, pageable)
                .map(TopicResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse getAdminById(UUID id) {
        return TopicResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public TopicResponse create(TopicRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", request.getSubjectId()));

        String name = request.getName().trim();
        if (topicRepository.existsBySubjectIdAndNameIgnoreCase(subject.getId(), name)) {
            throw new DuplicateResourceException("A topic named '" + name + "' already exists for this subject");
        }

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName(name);
        topic.setDescription(request.getDescription());
        topic.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        return TopicResponse.from(topicRepository.save(topic));
    }

    @Override
    @Transactional
    public TopicResponse update(UUID id, TopicRequest request) {
        Topic topic = findEntity(id);

        Subject subject = topic.getSubject();
        if (!subject.getId().equals(request.getSubjectId())) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Subject", request.getSubjectId()));
        }

        String name = request.getName().trim();
        boolean nameOrSubjectChanged = !name.equalsIgnoreCase(topic.getName()) || !subject.getId().equals(topic.getSubject().getId());
        if (nameOrSubjectChanged && topicRepository.existsBySubjectIdAndNameIgnoreCase(subject.getId(), name)) {
            throw new DuplicateResourceException("A topic named '" + name + "' already exists for this subject");
        }

        topic.setSubject(subject);
        topic.setName(name);
        topic.setDescription(request.getDescription());
        topic.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : topic.getDisplayOrder());

        return TopicResponse.from(topicRepository.save(topic));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Topic topic = findEntity(id);

        if (questionRepository.existsByTopicId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete this topic while questions are still attached to it");
        }

        topic.setDeletedAt(Instant.now());
        topicRepository.save(topic);
    }

    private Topic findEntity(UUID id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Topic", id));
    }
}
