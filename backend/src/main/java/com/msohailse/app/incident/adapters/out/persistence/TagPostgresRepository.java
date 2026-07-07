package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.domain.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class TagPostgresRepository implements TagRepositoryPort {

	@Inject
	EntityManager em;

	@Override
	public void save(Tag tag) {
		em.persist(tag);
	}

	@Override
	public Tag findById(int id) {
		return em.find(Tag.class, id);
	}

	@Override
	public Tag findByTitle(String title) {
		List<Tag> results = em.createQuery("select t from Tag t where t.tagTitle = :title", Tag.class)
				.setParameter("title", title)
				.getResultList();
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public List<Tag> findAll() {
		return em.createQuery("select t from Tag t", Tag.class).getResultList();
	}

	@Override
	public void update(Tag tag) {
		em.merge(tag);
	}

	@Override
	public void delete(int id) {
		Tag tag = em.find(Tag.class, id);
		if (tag != null) {
			em.remove(tag);
		}
	}
}
