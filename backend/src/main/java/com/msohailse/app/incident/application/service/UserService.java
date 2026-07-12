package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class UserService {

	@Inject
	UserRepositoryPort userRepository;

	@Inject
	DepartmentRepositoryPort departmentRepository;

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

	// Public /register above only ever creates a REPORTER — this is the only way an
	// ADMIN or another SUPER_ADMIN gets created, and only a SUPER_ADMIN may call it.
	@Transactional
	public User createUser(int actingUserId, String firstName, String lastName, String email, String password,
			UserType userType, Integer departmentId, Integer adminExpiresInDays) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || !actingUser.isSuperAdmin()) {
			throw new IllegalArgumentException("Only a super admin can create users");
		}
		if (userRepository.findByEmail(email) != null) {
			throw new IllegalArgumentException("Email already registered");
		}
		if (userType == UserType.ADMIN && departmentId == null) {
			throw new IllegalArgumentException("An admin must be assigned a department");
		}
		if (userType != UserType.ADMIN && (departmentId != null || adminExpiresInDays != null)) {
			throw new IllegalArgumentException("Only an admin can have a department or an expiry");
		}

		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setPassword(password);
		user.setUserType(userType);
		if (departmentId != null) {
			Department dept = departmentRepository.findById(departmentId);
			if (dept == null) {
				throw new IllegalArgumentException("Department not found: " + departmentId);
			}
			user.setDepartment(dept);
		}
		if (adminExpiresInDays != null) {
			user.setAdminExpiresAt(Date.from(Instant.now().plus(adminExpiresInDays, ChronoUnit.DAYS)));
		}
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
