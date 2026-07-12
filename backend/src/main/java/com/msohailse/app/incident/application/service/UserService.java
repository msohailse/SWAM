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

	// Public /register above only ever creates a REPORTER. This endpoint lets any active
	// admin create a REPORTER or an ADMIN — it deliberately never mints another SUPER_ADMIN:
	// that role only ever comes from startup seeding (see DataSeeder), because this whole
	// check trusts a client-supplied actingUserId with no real session behind it, and
	// SUPER_ADMIN is the one role that could otherwise be used to fully take over the system
	// through it. A department admin (not super) can only ever create another admin within
	// their own department — departmentId is forced to their own, never trusted from the
	// client, so the UI shouldn't even offer a choice for that case.
	@Transactional
	public User createUser(int actingUserId, String firstName, String lastName, String email, String password,
			UserType userType, Integer departmentId, Integer adminExpiresInDays) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || !actingUser.isActiveAdmin()) {
			throw new IllegalArgumentException("Only an admin can create users");
		}
		if (userType == UserType.SUPER_ADMIN) {
			throw new IllegalArgumentException("Cannot create another super admin through this endpoint");
		}
		if (!actingUser.isSuperAdmin() && userType == UserType.ADMIN) {
			Department actingDepartment = actingUser.getDepartment();
			if (actingDepartment == null) {
				throw new IllegalArgumentException("You must belong to a department to create another admin");
			}
			departmentId = actingDepartment.getId();
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
		if (adminExpiresInDays != null && adminExpiresInDays < 1) {
			throw new IllegalArgumentException("adminExpiresInDays must be positive");
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
		if (!user.verifyPassword(password)) {
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
