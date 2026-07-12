package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.User;
import java.util.List;

public interface IncidentRepositoryPort {
	void save(Incident incident);
	Incident findById(int id);
	List<Incident> findByUser(User user);

	// CQRS-lite read side: one flexible query for the read-only /incidents list, kept
	// separate from the plain findByUser() above so the simple callers don't need to pass
	// nulls. Any parameter left null means "don't filter on this" — department is how a
	// department-scoped admin's view gets narrowed server-side, not client-choosable.
	List<Incident> findFiltered(String tagTitle, Severity severity, Boolean closed, Department department);

	void update(Incident incident);
	void delete(int id);
}
