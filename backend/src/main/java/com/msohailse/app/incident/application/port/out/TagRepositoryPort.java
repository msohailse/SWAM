package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.Tag;
import java.util.List;

public interface TagRepositoryPort {
	void save(Tag tag);
	Tag findById(int id);
	Tag findByTitle(String title);
	List<Tag> findAll();
	void update(Tag tag);
	void delete(int id);
}
