package com.msohailse.app.incident.adapters.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class IncidentResourceTest {

	@Inject
	UserRepositoryPort userRepository;

	@Inject
	UserTransaction userTransaction;

	private int reporterId;
	private int adminId;

	// Setup writes directly through the repository port (skipping the REST layer) but must
	// really commit — the actual test calls go over HTTP, which runs in its own separate
	// transaction on the request thread, so this data must be visible there, not just in
	// this method's in-memory persistence context.
	@BeforeEach
	void setup() throws Exception {
		userTransaction.begin();
		User reporter = new User();
		reporter.setFirstName("John");
		reporter.setLastName("Doe");
		reporter.setEmail("reporter-" + System.nanoTime() + "@example.com");
		reporter.setPassword("SecurePass1");
		userRepository.save(reporter);
		reporterId = reporter.getId();

		User admin = new User();
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setEmail("admin-" + System.nanoTime() + "@example.com");
		admin.setPassword("SecurePass1");
		admin.setUserType(UserType.ADMIN);
		userRepository.save(admin);
		adminId = admin.getId();
		userTransaction.commit();
	}

	private String createIncidentBody() {
		return "{\"title\":\"Smoke detected\",\"description\":\"Server room\",\"severity\":\"HIGH\","
				+ "\"tagTitle\":\"fire-" + System.nanoTime() + "\",\"reportedByUserId\":" + reporterId + "}";
	}

	@Test
	void createFindUpdateCloseDeleteIncident() {
		int deptId = given()
				.contentType("application/json")
				.body("{\"name\":\"Support-" + System.nanoTime() + "\"}")
				.when().post("/departments")
				.then().statusCode(200)
				.extract().path("id");

		int id = given()
				.contentType("application/json")
				.body(createIncidentBody())
				.when().post("/incidents")
				.then().statusCode(200)
				.body("title", equalTo("Smoke detected"))
				.extract().path("id");

		given()
				.when().get("/incidents/" + id)
				.then().statusCode(200)
				.body("severity", equalTo("HIGH"));

		given()
				.contentType("application/json")
				.body("{\"title\":\"Smoke cleared\",\"description\":\"Resolved\",\"severity\":\"LOW\"}")
				.when().put("/incidents/" + id)
				.then().statusCode(200)
				.body("title", equalTo("Smoke cleared"))
				.body("severity", equalTo("LOW"));

		given()
				.contentType("application/json")
				.body("{\"actingUserId\":" + adminId + ",\"commentText\":\"Resolved, fixed the wiring\",\"assignedDepartmentId\":" + deptId + "}")
				.when().patch("/incidents/" + id + "/close")
				.then().statusCode(200)
				.body("closed", equalTo(true)); // Jackson maps isClosed() -> "closed", not "isClosed"

		given()
				.when().get("/incidents/" + id + "/comments")
				.then().statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].text", equalTo("Resolved, fixed the wiring"));

		given()
				.contentType("application/json")
				.body("{\"authorUserId\":" + reporterId + ",\"text\":\"Thanks, confirming it's fixed\"}")
				.when().post("/incidents/" + id + "/comments")
				.then().statusCode(200)
				.body("text", equalTo("Thanks, confirming it's fixed"));

		given()
				.when().get("/incidents/" + id + "/comments")
				.then().statusCode(200)
				.body("size()", equalTo(2))
				.body("[1].text", equalTo("Thanks, confirming it's fixed"));

		given()
				.when().delete("/incidents/" + id)
				.then().statusCode(204);
	}

	@Test
	void closeByNonAdminReturns400() {
		int id = given()
				.contentType("application/json")
				.body(createIncidentBody())
				.when().post("/incidents")
				.then().statusCode(200)
				.extract().path("id");

		given()
				.contentType("application/json")
				.body("{\"actingUserId\":" + reporterId + ",\"commentText\":\"trying to close my own\"}")
				.when().patch("/incidents/" + id + "/close")
				.then().statusCode(400);
	}

	@Test
	void findByUserReturnsIncidentsForThatUser() {
		given().contentType("application/json").body(createIncidentBody())
				.when().post("/incidents")
				.then().statusCode(200);

		given()
				.when().get("/incidents/user/" + reporterId)
				.then().statusCode(200)
				.body("size()", equalTo(1));
	}
}
