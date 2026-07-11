package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Department;
import java.util.List;

public interface DepartmentRepositoryPort {
	void save(Department department);
	Department findById(int id);
	Department findByName(String name);
	List<Department> findAll();
}
