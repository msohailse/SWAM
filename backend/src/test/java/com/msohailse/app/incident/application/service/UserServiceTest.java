package com.msohailse.app.incident.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserServiceTest {

	private static final String EMAIL = "john@example.com";
	private static final String PASSWORD = "SecurePass1";
	private static final String FIRST_NAME = "John";
	private static final String LAST_NAME = "Doe";

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepositoryPort userRepository;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@AfterEach
	void tearDown() throws Exception {
		closeable.close();
	}

	private User buildUser() {
		User user = new User();
		user.setFirstName(FIRST_NAME);
		user.setLastName(LAST_NAME);
		user.setEmail(EMAIL);
		user.setPassword(PASSWORD);
		return user;
	}

	@Test
	void loginWhenUserNotFoundThrows() {
		when(userRepository.findByEmail(EMAIL)).thenReturn(null);

		assertThatThrownBy(() -> userService.login(EMAIL, PASSWORD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not found");
	}

	@Test
	void loginWhenPasswordWrongThrows() {
		when(userRepository.findByEmail(EMAIL)).thenReturn(buildUser());

		assertThatThrownBy(() -> userService.login(EMAIL, "WrongPass99"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid password");
	}

	@Test
	void loginWhenValidReturnsUser() {
		User user = buildUser();
		when(userRepository.findByEmail(EMAIL)).thenReturn(user);

		User loggedIn = userService.login(EMAIL, PASSWORD);

		assertThat(loggedIn).isSameAs(user);
	}

	@Test
	void registerWhenEmailAlreadyExistsThrows() {
		when(userRepository.findByEmail(EMAIL)).thenReturn(buildUser());

		assertThatThrownBy(() -> userService.register(FIRST_NAME, LAST_NAME, EMAIL, PASSWORD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email already registered");
	}

	@Test
	void registerWhenValidSavesUser() {
		when(userRepository.findByEmail(EMAIL)).thenReturn(null);

		userService.register(FIRST_NAME, LAST_NAME, EMAIL, PASSWORD);

		InOrder inOrder = inOrder(userRepository);
		inOrder.verify(userRepository).findByEmail(EMAIL);
		inOrder.verify(userRepository).save(any(User.class));
	}

	@Test
	void registerWhenInvalidEmailThrowsFromUserEntity() {
		when(userRepository.findByEmail("notanemail")).thenReturn(null);

		assertThatThrownBy(() -> userService.register(FIRST_NAME, LAST_NAME, "notanemail", PASSWORD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid email format");
	}
}
