package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class UserPostgresRepository implements UserRepositoryPort {

	@Inject
	EntityManager em;

	@Override
	public void save(User user) {
		if (em.contains(user) || user.getId() != 0) {
			em.merge(user);
		} else {
			em.persist(user);
		}
	}

	@Override
	public User findById(int id) {
		return em.find(User.class, id);
	}

	@Override
	public List<User> findAll() {
		return em.createQuery("select u from User u", User.class).getResultList();
	}

	@Override
	public User findByEmail(String email) {
		List<User> results = em.createQuery("select u from User u where u.email = :email", User.class)
				.setParameter("email", email)
				.getResultList();
		return results.isEmpty() ? null : results.get(0);
	}
}
