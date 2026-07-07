package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.IncidentRepositoryPort;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class IncidentPostgresRepository implements IncidentRepositoryPort {

	@Inject
	EntityManager em;

	@Override
	public void save(Incident incident) {
		em.persist(incident);
	}

	@Override
	public Incident findById(int id) {
		return em.find(Incident.class, id);
	}

	@Override
	public List<Incident> findAll() {
		return em.createQuery("select i from Incident i", Incident.class).getResultList();
	}

	@Override
	public List<Incident> findByUser(User user) {
		return em.createQuery("select i from Incident i where i.reportedBy = :user", Incident.class)
				.setParameter("user", user)
				.getResultList();
	}

	@Override
	public void update(Incident incident) {
		em.merge(incident);
	}

	@Override
	public void delete(int id) {
		Incident incident = em.find(Incident.class, id);
		if (incident != null) {
			// Comments reference the incident via a FK — delete them first, or the incident
			// delete below would violate that constraint.
			em.createQuery("delete from Comment c where c.incident = :incident")
					.setParameter("incident", incident)
					.executeUpdate();
			em.remove(incident);
		}
	}
}
