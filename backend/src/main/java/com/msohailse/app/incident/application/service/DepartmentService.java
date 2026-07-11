package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class DepartmentService {

	@Inject
	DepartmentRepositoryPort departmentRepository;

	@Transactional
	public Department create(String name, String description) {
		if (departmentRepository.findByName(name) != null) {
			throw new IllegalArgumentException("Department with name " + name + " already exists");
		}
		Department department = new Department();
		department.setName(name);
		department.setDescription(description);
		departmentRepository.save(department);
		return department;
	}

	public Department findById(int id) {
		return departmentRepository.findById(id);
	}

	public List<Department> findAll() {
		return departmentRepository.findAll();
	}
}
