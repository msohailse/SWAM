package com.msohailse.app.incident.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TagTest {

	private Tag firstTag;

	@BeforeEach
	void setup() {
		firstTag = new Tag();
	}

	@Test
	public void testTagTitleWhenValidShouldStoreTitle() {
		firstTag.setTagTitle("Fire");
		assertEquals("Fire", firstTag.getTagTitle());
	}

	@Test
	public void testTagTitleWithNull() {
		try {
			firstTag.setTagTitle(null);
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Empty title", e.getMessage());
		}
	}

	@Test
	public void testTagTitleWithOnlySpacesShouldThrow() {
		try {
			firstTag.setTagTitle("   ");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Empty title", e.getMessage());
		}
	}

	@Test
	public void testTagTitleWithLeadingSpaceIsNormalized() {
		firstTag.setTagTitle(" fire");
		assertEquals("fire", firstTag.getTagTitle());
	}

	@Test
	public void testTagDescriptionWhenSetShouldStoreDescription() {
		firstTag.setTagDescription("Incidents related to fire or smoke");
		assertEquals("Incidents related to fire or smoke", firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionCanBeNull() {
		firstTag.setTagDescription(null);
		assertNull(firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionWithLeadingSpaceIsNormalized() {
		firstTag.setTagDescription(" fire related");
		assertEquals("fire related", firstTag.getTagDescription());
	}
}
