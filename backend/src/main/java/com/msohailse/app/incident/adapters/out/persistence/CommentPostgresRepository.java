package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.CommentRepositoryPort;
import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Incident;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class CommentPostgresRepository implements CommentRepositoryPort {

	@Inject
	EntityManager em;

	@Override
	public void save(Comment comment) {
		em.persist(comment);
	}

	@Override
	public List<Comment> findByIncident(Incident incident) {
		return em.createQuery("select c from Comment c where c.incident = :incident order by c.createdAt", Comment.class)
				.setParameter("incident", incident)
				.getResultList();
	}
}
