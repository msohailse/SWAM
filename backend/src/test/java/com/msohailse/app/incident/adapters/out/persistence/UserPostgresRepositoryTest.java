package com.msohailse.app.incident.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msohailse.app.incident.domain.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class UserPostgresRepositoryTest {

	@Inject
	UserPostgresRepository userRepo;

	@Inject
	EntityManager em;

	private User buildUser(String email) {
		User user = new User();
		user.setFirstName("John");
		user.setLastName("Doe");
		user.setEmail(email);
		user.setPassword("SecurePass123");
		return user;
	}

	@Test
	@TestTransaction
	void saveAssignsAnId() {
		User user = buildUser("john@example.com");

		userRepo.save(user);

		assertThat(user.getId()).isGreaterThan(0);
	}

	@Test
	@TestTransaction
	void findByIdReturnsSavedUser() {
		User user = buildUser("jane@example.com");
		user.setFirstName("Jane");
		user.setLastName("Smith");
		userRepo.save(user);

		User retrieved = userRepo.findById(user.getId());

		assertThat(retrieved.getFirstName()).isEqualTo("Jane");
		assertThat(retrieved.getLastName()).isEqualTo("Smith");
		assertThat(retrieved.getEmail()).isEqualTo("jane@example.com");
	}

	@Test
	@TestTransaction
	void findAllReturnsAllSavedUsers() {
		userRepo.save(buildUser("alice@example.com"));
		userRepo.save(buildUser("bob@example.com"));

		assertThat(userRepo.findAll()).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	@TestTransaction
	void saveWithDuplicateEmailThrows() {
		userRepo.save(buildUser("duplicate@example.com"));
		em.flush();

		assertThatThrownBy(() -> {
			userRepo.save(buildUser("duplicate@example.com"));
			em.flush();
		}).isInstanceOf(RuntimeException.class);
	}
}
