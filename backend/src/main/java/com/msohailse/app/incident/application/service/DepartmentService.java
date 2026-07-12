package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class DepartmentService {

	@Inject
	DepartmentRepositoryPort departmentRepository;

	@Inject
	UserRepositoryPort userRepository;

	@Transactional
	public Department create(int actingUserId, String name, String description) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || !actingUser.isActiveAdmin()) {
			throw new IllegalArgumentException("Only an admin can manage departments");
		}
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
