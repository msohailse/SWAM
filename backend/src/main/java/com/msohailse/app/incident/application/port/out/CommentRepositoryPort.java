package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Incident;
import java.util.List;

public interface CommentRepositoryPort {
	void save(Comment comment);
	List<Comment> findByIncident(Incident incident);
}
