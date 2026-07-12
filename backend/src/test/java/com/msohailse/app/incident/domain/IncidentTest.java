package com.msohailse.app.incident.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IncidentTest {

	private Incident firstIncident;

	@BeforeEach
	void setup() {
		firstIncident = new Incident();
	}

	@Test
	public void testReportedAtIsSetOnConstruction() {
		assertNotNull(firstIncident.getReportedAt(), "reportedAt should be set automatically in constructor");
	}

	@Test
	public void testTitleWhenValidShouldStoreTitle() {
		firstIncident.setTitle("Server is down");
		assertEquals("Server is down", firstIncident.getTitle());
	}

	@Test
	public void testTitleWithNullShouldThrow() {
		try {
			firstIncident.setTitle(null);
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Empty title", e.getMessage());
		}
	}

	@Test
	public void testTitleWithOnlySpacesShouldThrow() {
		try {
			firstIncident.setTitle("   ");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Empty title", e.getMessage());
		}
	}

	@Test
	public void testTitleWithLeadingSpaceIsNormalized() {
		firstIncident.setTitle(" fire alarm");
		assertEquals("fire alarm", firstIncident.getTitle());
	}

	@Test
	public void testDescriptionCanBeNull() {
		firstIncident.setDescription(null);
		assertNull(firstIncident.getDescription());
	}

	@Test
	public void testDescriptionWhenSetShouldStoreDescription() {
		firstIncident.setDescription("Smoke detected on the second floor near the server room");
		assertEquals("Smoke detected on the second floor near the server room", firstIncident.getDescription());
	}

	@Test
	public void testDescriptionWithLeadingAndTrailingSpacesIsNormalized() {
		firstIncident.setDescription("  broken window  ");
		assertEquals("broken window", firstIncident.getDescription());
	}

	@Test
	public void testSeverityNullShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstIncident.setSeverity(null));
		assertEquals("Empty severity", e.getMessage());
	}

	@Test
	public void testReportedByWhenValidShouldStore() {
		User user = new User();
		user.setFirstName("Alice");
		user.setLastName("Smith");
		user.setEmail("alice@example.com");
		user.setPassword("AlicePass1");
		firstIncident.setReportedBy(user);
		assertEquals(user, firstIncident.getReportedBy());
	}

	@Test
	public void testReportedByNullShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstIncident.setReportedBy(null));
		assertEquals("Reporter cannot be null", e.getMessage());
	}

	@Test
	public void testSetTagWhenValidShouldStore() {
		Tag tag = new Tag();
		tag.setTagTitle("fire");
		firstIncident.setTag(tag);
		assertEquals(tag, firstIncident.getTag());
	}

	@Test
	public void testSetTagNullShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstIncident.setTag(null));
		assertEquals("Tag cannot be null", e.getMessage());
	}

	@Test
	public void testSetIsClosedTrueSetsFlag() {
		firstIncident.setIsClosed(true);
		assertTrue(firstIncident.isClosed());
	}
}
