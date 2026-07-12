package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.IncidentRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
	public List<Incident> findByUser(User user) {
		return em.createQuery("select i from Incident i where i.reportedBy = :user", Incident.class)
				.setParameter("user", user)
				.getResultList();
	}

	@Override
	public List<Incident> findFiltered(String tagTitle, Severity severity, Boolean closed, Department department) {
		// Plain JPQL, built up with a StringBuilder — no Criteria API/Specifications.
		// Each filter is optional: only append the clause (and bind the parameter) if the
		// caller actually asked for it.
		StringBuilder jpql = new StringBuilder("select i from Incident i where 1=1");
		if (tagTitle != null) {
			jpql.append(" and i.tag.tagTitle = :tagTitle");
		}
		if (severity != null) {
			jpql.append(" and i.severity = :severity");
		}
		if (closed != null) {
			jpql.append(" and i.isClosed = :closed");
		}
		if (department != null) {
			jpql.append(" and i.assignedDepartment = :department");
		}

		TypedQuery<Incident> query = em.createQuery(jpql.toString(), Incident.class);
		if (tagTitle != null) {
			query.setParameter("tagTitle", tagTitle);
		}
		if (severity != null) {
			query.setParameter("severity", severity);
		}
		if (closed != null) {
			query.setParameter("closed", closed);
		}
		if (department != null) {
			query.setParameter("department", department);
		}
		return query.getResultList();
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
