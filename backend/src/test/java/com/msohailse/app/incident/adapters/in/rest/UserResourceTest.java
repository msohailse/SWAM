package com.msohailse.app.incident.adapters.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

// No @TestTransaction here on purpose: these calls go over real HTTP to the running
// Quarkus test server, which commits for real on its own request thread — a test-method
// transaction wrapper here wouldn't roll any of that back. Each test uses its own unique
// email instead, since data genuinely persists across test methods.
@QuarkusTest
public class UserResourceTest {

	private String registerBody(String email) {
		return "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"" + email + "\",\"password\":\"SecurePass1\"}";
	}

	@Test
	void registerThenLoginSucceeds() {
		String email = "register-login-" + System.nanoTime() + "@example.com";

		given()
				.contentType("application/json")
				.body(registerBody(email))
				.when().post("/users/register")
				.then().statusCode(200)
				.body("email", equalTo(email))
				.body("password", equalTo(null)); // never exposed in responses

		given()
				.contentType("application/json")
				.body("{\"email\":\"" + email + "\",\"password\":\"SecurePass1\"}")
				.when().post("/users/login")
				.then().statusCode(200)
				.body("firstName", equalTo("John"));
	}

	@Test
	void loginWithWrongPasswordReturns400() {
		String email = "wrong-password-" + System.nanoTime() + "@example.com";

		given()
				.contentType("application/json")
				.body(registerBody(email))
				.when().post("/users/register")
				.then().statusCode(200);

		given()
				.contentType("application/json")
				.body("{\"email\":\"" + email + "\",\"password\":\"WrongPass1\"}")
				.when().post("/users/login")
				.then().statusCode(400)
				.body("error", equalTo("Invalid password"));
	}

	@Test
	void registerWithDuplicateEmailReturns400() {
		String email = "duplicate-" + System.nanoTime() + "@example.com";

		given().contentType("application/json").body(registerBody(email))
				.when().post("/users/register")
				.then().statusCode(200);

		given().contentType("application/json").body(registerBody(email))
				.when().post("/users/register")
				.then().statusCode(400)
				.body("error", equalTo("Email already registered"));
	}
}
