package com.msohailse.app.incident.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {

	private User firstUser;

	@BeforeEach
	void setup() {
		firstUser = new User();
	}

	@Test
	public void testFirstNameWithWhiteSpaceAtEnd() throws Exception {
		try {
			firstUser.setFirstName("Sohail ");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("White space found", e.getMessage());
		}
	}

	@Test
	public void testLastNameWithWhiteSpaceAtEnd() {
		try {
			firstUser.setLastName("John ");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("White space found", e.getMessage());
		}
	}

	@Test
	public void testEmailWithEmptyString() {
		try {
			firstUser.setEmail("");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Empty email", e.getMessage());
		}
	}

	@Test
	public void testEmailWithoutAtRate() {
		try {
			firstUser.setEmail("msohailgmail.com");
			fail("Expected an IllegalArgumentException to be thrown");
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid email format", e.getMessage());
		}
	}

	@Test
	public void testSetPasswordWhenNullShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> firstUser.setPassword(null));
		assertEquals("Empty password", e.getMessage());
	}

	@Test
	public void testSetPasswordWhenTooShortShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstUser.setPassword("Ab1cdef"));
		assertEquals("Password too short", e.getMessage());
	}

	@Test
	public void testSetPasswordWithoutDigitShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstUser.setPassword("Abcdefgh"));
		assertEquals("Password must contain a digit", e.getMessage());
	}

	@Test
	public void testSetPasswordWithoutUppercaseShouldThrow() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> firstUser.setPassword("abcdefg1"));
		assertEquals("Password must contain an uppercase letter", e.getMessage());
	}

	@Test
	public void testSetPasswordWhenValidShouldStoreHashNotPlaintext() {
		firstUser.setPassword("Abcdefg1");
		assertNotEquals("Abcdefg1", firstUser.getPassword());
		assertTrue(firstUser.verifyPassword("Abcdefg1"));
		assertFalse(firstUser.verifyPassword("WrongPass1"));
	}
}
