package com.msohailse.app.incident.adapters.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TagResourceTest {

	@Inject
	UserRepositoryPort userRepository;

	@Inject
	UserTransaction userTransaction;

	private int adminId;
	private int reporterId;

	@BeforeEach
	void setup() throws Exception {
		userTransaction.begin();
		User admin = new User();
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setEmail("tag-admin-" + System.nanoTime() + "@example.com");
		admin.setPassword("SecurePass1");
		admin.setUserType(UserType.SUPER_ADMIN);
		userRepository.save(admin);
		adminId = admin.getId();

		User reporter = new User();
		reporter.setFirstName("Reporter");
		reporter.setLastName("User");
		reporter.setEmail("tag-reporter-" + System.nanoTime() + "@example.com");
		reporter.setPassword("SecurePass1");
		userRepository.save(reporter);
		reporterId = reporter.getId();
		userTransaction.commit();
	}

	@Test
	void createFindUpdateDeleteTag() {
		String title = "flood-" + System.nanoTime();

		int id = given()
				.contentType("application/json")
				.body("{\"actingUserId\":" + adminId + ",\"tagTitle\":\"" + title + "\",\"tagDescription\":\"flood-related incidents\"}")
				.when().post("/tags")
				.then().statusCode(200)
				.body("tagTitle", equalTo(title))
				.extract().path("id");

		given()
				.when().get("/tags/" + id)
				.then().statusCode(200)
				.body("tagTitle", equalTo(title));

		String updatedTitle = title + "-updated";
		given()
				.contentType("application/json")
				.body("{\"actingUserId\":" + adminId + ",\"tagTitle\":\"" + updatedTitle + "\",\"tagDescription\":\"still flood\"}")
				.when().put("/tags/" + id)
				.then().statusCode(200)
				.body("tagTitle", equalTo(updatedTitle));

		given()
				.when().get("/tags")
				.then().statusCode(200)
				.body("size()", greaterThan(0));

		given()
				.when().delete("/tags/" + id + "?actingUserId=" + adminId)
				.then().statusCode(204);

		given()
				.when().get("/tags/" + id)
				.then().statusCode(204);
	}

	@Test
	void createTagByNonAdminReturns400() {
		given()
				.contentType("application/json")
				.body("{\"actingUserId\":" + reporterId + ",\"tagTitle\":\"blocked-" + System.nanoTime() + "\",\"tagDescription\":\"nope\"}")
				.when().post("/tags")
				.then().statusCode(400);
	}
}
