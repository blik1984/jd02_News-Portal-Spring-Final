package com.epam.edu.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epam.edu.dao.impl.CommentRepositoryImpl;
import com.epam.edu.entity.Comment;
import com.epam.edu.service.CommentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

	private final CommentRepositoryImpl repository;

	@Override
	@Transactional
	public void save(Comment comment) {
		log.info("Saving comment: id={}, newsId={}, userId={}", comment.getId(),
				comment.getNews() != null ? comment.getNews().getId() : null,
				comment.getUser() != null ? comment.getUser().getId() : null);
		repository.save(comment);
		log.info("Comment saved successfully: id={}", comment.getId());
	}

	@Override
	@Transactional
	public void delete(Long id) {
		log.info("Deleting comment: id={}", id);
		repository.deleteById(id);
		log.info("Comment deleted successfully: id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Comment> findById(Long id) {
		log.info("Fetching comment by id={}", id);
		Optional<Comment> commentOpt = repository.findById(id);
		if (commentOpt.isPresent()) {
			log.debug("Comment found: id={}", id);
		} else {
			log.warn("Comment not found: id={}", id);
		}
		return commentOpt;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> findAllByNewsId(Long newsId) {
		log.info("Fetching all comments for newsId={}", newsId);
		List<Comment> comments = repository.findAllByNewsId(newsId);
		log.debug("Number of comments found for newsId={}: {}", newsId, comments.size());
		return comments;
	}
}
