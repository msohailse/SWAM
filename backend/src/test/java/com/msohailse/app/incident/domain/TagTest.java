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
	public void testIdDefaultsToZeroBeforePersistence() {
		assertTrue(firstTag.getId() == 0, "Id should default to 0 before JPA persists");
	}

	@Test
	public void testTagTitleDefaultsToNull() {
		assertNull(firstTag.getTagTitle());
	}

	@Test
	public void testTagDescriptionDefaultsToNull() {
		assertNull(firstTag.getTagDescription());
	}

	@Test
	public void testTagTitleWhenValidShouldStoreTitle() {
		firstTag.setTagTitle("Fire");
		assertEquals("Fire", firstTag.getTagTitle());
	}

	@Test
	public void testTagTitleWithSingleSpaceIsValid() {
		firstTag.setTagTitle("fire alarm");
		assertEquals("fire alarm", firstTag.getTagTitle());
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
	public void testTagTitleWithEmptyString() {
		try {
			firstTag.setTagTitle("");
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
	public void testTagTitleWithTrailingSpaceIsNormalized() {
		firstTag.setTagTitle("theft ");
		assertEquals("theft", firstTag.getTagTitle());
	}

	@Test
	public void testTagTitleWithMultipleSpacesInMiddleIsNormalized() {
		firstTag.setTagTitle("fire  alarm");
		assertEquals("fire alarm", firstTag.getTagTitle());
	}

	@Test
	public void testTagTitleWithTabCharacterIsNormalized() {
		firstTag.setTagTitle("fire\talarm");
		assertEquals("fire alarm", firstTag.getTagTitle());
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
	public void testTagDescriptionCanBeEmptyString() {
		firstTag.setTagDescription("");
		assertEquals("", firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionWithLeadingSpaceIsNormalized() {
		firstTag.setTagDescription(" fire related");
		assertEquals("fire related", firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionWithTrailingSpaceIsNormalized() {
		firstTag.setTagDescription("theft incident ");
		assertEquals("theft incident", firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionWithMultipleSpacesInMiddleIsNormalized() {
		firstTag.setTagDescription("fire  related  incident");
		assertEquals("fire related incident", firstTag.getTagDescription());
	}

	@Test
	public void testTagDescriptionWithMultipleSpacesInMiddleAndEitherSidesIsNormalized() {
		firstTag.setTagDescription(" fire  related  incident ");
		assertEquals("fire related incident", firstTag.getTagDescription());
	}
}
