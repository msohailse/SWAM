package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class DepartmentPostgresRepository implements DepartmentRepositoryPort {

	@Inject
	EntityManager em;

	@Override
	public void save(Department department) {
		em.persist(department);
	}

	@Override
	public Department findById(int id) {
		return em.find(Department.class, id);
	}

	@Override
	public Department findByName(String name) {
		List<Department> deps = em.createQuery("select d from Department d where d.name = :name", Department.class)
				.setParameter("name", name)
				.getResultList();
		return deps.isEmpty() ? null : deps.get(0);
	}

	@Override
	public List<Department> findAll() {
		return em.createQuery("select d from Department d", Department.class).getResultList();
	}
}
