package com.msohailse.app.incident.adapters.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TagResourceTest {

	@Test
	void createFindUpdateDeleteTag() {
		String title = "flood-" + System.nanoTime();

		int id = given()
				.contentType("application/json")
				.body("{\"tagTitle\":\"" + title + "\",\"tagDescription\":\"flood-related incidents\"}")
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
				.body("{\"tagTitle\":\"" + updatedTitle + "\",\"tagDescription\":\"still flood\"}")
				.when().put("/tags/" + id)
				.then().statusCode(200)
				.body("tagTitle", equalTo(updatedTitle));

		given()
				.when().get("/tags")
				.then().statusCode(200)
				.body("size()", greaterThan(0));

		given()
				.when().delete("/tags/" + id)
				.then().statusCode(204);

		given()
				.when().get("/tags/" + id)
				.then().statusCode(204);
	}
}
