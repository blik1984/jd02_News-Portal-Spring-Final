package com.epam.edu.dao.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import com.epam.edu.dao.CommentRepository;
import com.epam.edu.entity.Comment;

@Repository
public class CommentRepositoryImpl
		extends NewsPortalBaseRepository<Comment, Long>
		implements CommentRepository {

	protected CommentRepositoryImpl() {
		super(Comment.class);
	}

	@Override
	public void deleteAllByNewsId(Long newsId) {
		Session session = entityManager.unwrap(Session.class);

		String hql = """
			DELETE FROM Comment c
			WHERE c.news.id = :newsId
		""";

		MutationQuery query = session.createMutationQuery(hql);
		query.setParameter("newsId", newsId);
		query.executeUpdate();
	}

	@Override
	public List<Comment> findAllByNewsId(Long newsId) {
		Session session = entityManager.unwrap(Session.class);

		String hql = """
			SELECT c
			FROM Comment c
			WHERE c.news.id = :newsId
			ORDER BY c.createdAt ASC
		""";

		Query<Comment> query = session.createQuery(hql, Comment.class);
		query.setParameter("newsId", newsId);

		return query.getResultList();
	}
}
