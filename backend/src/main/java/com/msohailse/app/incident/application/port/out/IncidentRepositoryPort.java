package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.User;
import java.util.List;

public interface IncidentRepositoryPort {
	void save(Incident incident);
	Incident findById(int id);
	List<Incident> findAll();
	List<Incident> findByUser(User user);
	void update(Incident incident);
	void delete(int id);
}
