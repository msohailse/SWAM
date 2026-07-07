package com.msohailse.app.incident.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.Tag;
import com.msohailse.app.incident.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// No @TestTransaction: each test method wraps its own work in a real, committed
// UserTransaction instead. Every test uses unique emails/tag titles (System.nanoTime())
// so committed data never collides across methods or test classes.
@QuarkusTest
public class IncidentPostgresRepositoryTest {

	@Inject
	IncidentPostgresRepository incidentRepo;

	@Inject
	UserPostgresRepository userRepo;

	@Inject
	TagPostgresRepository tagRepo;

	@Inject
	EntityManager em;

	@Inject
	UserTransaction userTransaction;

	private User helperUser;
	private Tag helperTag;

	@BeforeEach
	void setup() throws Exception {
		userTransaction.begin();

		helperUser = new User();
		helperUser.setFirstName("John");
		helperUser.setLastName("Doe");
		helperUser.setEmail("john-" + System.nanoTime() + "@example.com");
		helperUser.setPassword("SecurePass123");
		userRepo.save(helperUser);

		helperTag = new Tag();
		helperTag.setTagTitle("fire-" + System.nanoTime());
		tagRepo.save(helperTag);
	}

	// Commits the transaction opened in setup() — the whole test method (setup + test body)
	// runs as one real transaction, then commits here instead of rolling back.
	@AfterEach
	void tearDown() throws Exception {
		userTransaction.commit();
	}

	private Incident buildIncident(String title, Severity severity) {
		Incident incident = new Incident();
		incident.setTitle(title);
		incident.setSeverity(severity);
		incident.setReportedBy(helperUser);
		incident.setTag(helperTag);
		return incident;
	}

	@Test
	void saveAssignsAnId() {
		Incident incident = buildIncident("Server room overheating", Severity.HIGH);

		incidentRepo.save(incident);

		assertThat(incident.getId()).isGreaterThan(0);
	}

	@Test
	void findByIdReturnsCorrectIncident() {
		Incident incident = buildIncident("Broken window", Severity.MEDIUM);
		incidentRepo.save(incident);

		Incident retrieved = incidentRepo.findById(incident.getId());

		assertThat(retrieved.getTitle()).isEqualTo("Broken window");
		assertThat(retrieved.getSeverity()).isEqualTo(Severity.MEDIUM);
		assertThat(retrieved.getReportedBy().getFirstName()).isEqualTo("John");
	}

	@Test
	void tagRelationshipPersists() {
		Incident incident = buildIncident("Smoke detected", Severity.HIGH);
		incidentRepo.save(incident);

		Incident retrieved = incidentRepo.findById(incident.getId());

		assertThat(retrieved.getTag().getTagTitle()).isEqualTo(helperTag.getTagTitle());
	}

	@Test
	void isClosedDefaultsFalseAfterPersist() {
		Incident incident = buildIncident("Door left open", Severity.LOW);
		incidentRepo.save(incident);

		Incident retrieved = incidentRepo.findById(incident.getId());

		assertThat(retrieved.isClosed()).isFalse();
	}

	@Test
	void updateChangesClosedFlag() {
		Incident incident = buildIncident("Leaky faucet", Severity.LOW);
		incidentRepo.save(incident);

		incident.setIsClosed(true);
		incidentRepo.update(incident);

		assertThat(incidentRepo.findById(incident.getId()).isClosed()).isTrue();
	}

	@Test
	void deleteRemovesIncident() {
		Incident incident = buildIncident("To be deleted", Severity.LOW);
		incidentRepo.save(incident);
		int id = incident.getId();

		incidentRepo.delete(id);

		assertThat(incidentRepo.findById(id)).isNull();
	}

	@Test
	void findByUserReturnsOnlyThatUsersIncidents() {
		incidentRepo.save(buildIncident("Water leak", Severity.LOW));
		incidentRepo.save(buildIncident("Power outage", Severity.HIGH));
		em.flush();

		List<Incident> byUser = incidentRepo.findByUser(helperUser);

		assertThat(byUser).hasSize(2);
	}

	@Test
	void findByIdReturnsNullForNonExistentId() {
		assertThat(incidentRepo.findById(99999)).isNull();
	}
}
