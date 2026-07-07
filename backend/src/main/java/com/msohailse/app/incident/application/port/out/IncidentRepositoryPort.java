package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.User;
import java.util.List;

public interface IncidentRepositoryPort {
	void save(Incident incident);
	Incident findById(int id);
	List<Incident> findAll();
	List<Incident> findByUser(User user);

	// CQRS-lite read side: one flexible query for the read-only /incidents list, kept
	// separate from the plain findAll()/findByUser() above so the simple callers don't
	// need to pass three nulls. Any parameter left null means "don't filter on this".
	List<Incident> findFiltered(String tagTitle, Severity severity, Boolean closed);

	void update(Incident incident);
	void delete(int id);
}
