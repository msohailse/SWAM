package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class UserService {

	@Inject
	UserRepositoryPort userRepository;

	@Transactional
	public User register(String firstName, String lastName, String email, String password) {
		if (userRepository.findByEmail(email) != null) {
			throw new IllegalArgumentException("Email already registered");
		}
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setPassword(password);
		userRepository.save(user);
		return user;
	}

	public User login(String email, String password) {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User not found");
		}
		if (!user.getPassword().equals(password)) {
			throw new IllegalArgumentException("Invalid password");
		}
		return user;
	}

	public User findById(int id) {
		return userRepository.findById(id);
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}
}
