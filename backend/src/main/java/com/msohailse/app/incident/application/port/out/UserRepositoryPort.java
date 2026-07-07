package com.msohailse.app.incident.application.port.out;

import com.msohailse.app.incident.domain.User;
import java.util.List;

public interface UserRepositoryPort {
	void save(User user);
	User findById(int id);
	User findByEmail(String email);
	List<User> findAll();
}
