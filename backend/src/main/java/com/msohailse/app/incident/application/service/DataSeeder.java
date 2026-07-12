package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import com.msohailse.app.incident.domain.Tag;
import com.msohailse.app.incident.domain.Department;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

// Seeds one known admin and one known reporter at startup, so the app is usable right
// after `docker compose up` — registration only ever creates REPORTERs, so without this
// the first admin would need manual SQL. Credentials come from deploy/.env (SEED_* vars,
// falling back to the defaults in application.properties); disabled under `mvn test`.
@ApplicationScoped
public class DataSeeder {

	private static final Logger LOG = Logger.getLogger(DataSeeder.class);

	@Inject
	UserRepositoryPort userRepository;

	@Inject
	TagRepositoryPort tagRepository;

	@Inject
	DepartmentRepositoryPort departmentRepository;

	@ConfigProperty(name = "seed.enabled", defaultValue = "true")
	boolean enabled;

	@ConfigProperty(name = "seed.admin.email")
	String adminEmail;

	@ConfigProperty(name = "seed.admin.password")
	String adminPassword;

	@ConfigProperty(name = "seed.user.email")
	String userEmail;

	@ConfigProperty(name = "seed.user.password")
	String userPassword;

	@ConfigProperty(name = "seed.dept-admin.email")
	String deptAdminEmail;

	@ConfigProperty(name = "seed.dept-admin.password")
	String deptAdminPassword;

	@Transactional
	void onStart(@Observes StartupEvent event) {
		if (!enabled) {
			return;
		}
		seedTag("Software", "Software issues and bugs");
		seedTag("Hardware", "Hardware failures and replacements");
		seedTag("Network", "Connectivity and infrastructure issues");

		Department itSupport = seedDepartment("IT Support", "Technical and IT helpdesk");
		seedDepartment("Facilities", "Building and equipment maintenance");
		seedDepartment("Human Resources", "Personnel and employee relations");

		// Sees/manages every incident and is the only type that can assign/reassign a
		// department or create other users.
		seed("Super", "Admin", adminEmail, adminPassword, UserType.SUPER_ADMIN, null);
		// A department admin, for the scoped-permissions demo: only sees/closes incidents
		// assigned to IT Support. No expiry — a stable always-on demo account; time-boxed
		// admins are created via the super admin's own user-creation screen at runtime.
		seed("IT", "Admin", deptAdminEmail, deptAdminPassword, UserType.ADMIN, itSupport);
		seed("Demo", "Reporter", userEmail, userPassword, UserType.REPORTER, null);
	}

	// Idempotent: an already-existing email is left untouched, so restarts (and extra
	// replicas, apart from a harmless first-boot race) never duplicate or overwrite users.
	private void seed(String firstName, String lastName, String email, String password, UserType type, Department department) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			LOG.warn("Skipping " + type + " seed user: email/password not configured");
			return;
		}
		if (userRepository.findByEmail(email) != null) {
			return;
		}
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setPassword(password);
		user.setUserType(type);
		user.setDepartment(department);
		userRepository.save(user);
		LOG.info("Seeded " + type + " user " + email + (department != null ? " (department: " + department.getName() + ")" : ""));
	}

	private void seedTag(String title, String description) {
		if (tagRepository.findByTitle(title) != null) {
			return;
		}
		Tag tag = new Tag();
		tag.setTagTitle(title);
		tag.setTagDescription(description);
		tagRepository.save(tag);
		LOG.info("Seeded tag: " + title);
	}

	private Department seedDepartment(String name, String description) {
		Department existing = departmentRepository.findByName(name);
		if (existing != null) {
			return existing;
		}
		Department dept = new Department();
		dept.setName(name);
		dept.setDescription(description);
		departmentRepository.save(dept);
		LOG.info("Seeded department: " + name);
		return dept;
	}
}
