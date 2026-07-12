package com.msohailse.app.incident.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

	@Mock
	private DepartmentRepositoryPort departmentRepository;

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

	@Test
	void createUserByDepartmentAdminForcesOwnDepartmentIgnoringRequestedOne() throws Exception {
		Department ownDept = new Department();
		setDepartmentId(ownDept, 1);
		Department otherDept = new Department();
		setDepartmentId(otherDept, 2);

		User deptAdmin = new User();
		deptAdmin.setUserType(UserType.ADMIN);
		deptAdmin.setDepartment(ownDept);
		when(userRepository.findById(9)).thenReturn(deptAdmin);
		when(userRepository.findByEmail(EMAIL)).thenReturn(null);
		when(departmentRepository.findById(1)).thenReturn(ownDept);

		userService.createUser(9, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, UserType.ADMIN, otherDept.getId(), null);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getDepartment()).isEqualTo(ownDept);
	}

	@Test
	void createUserByDepartmentAdminCanCreateReporterWithNoDepartment() {
		User deptAdmin = new User();
		deptAdmin.setUserType(UserType.ADMIN);
		when(userRepository.findById(9)).thenReturn(deptAdmin);
		when(userRepository.findByEmail(EMAIL)).thenReturn(null);

		userService.createUser(9, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, UserType.REPORTER, null, null);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getDepartment()).isNull();
	}

	@Test
	void createUserByReporterThrows() {
		User reporter = buildUser();
		when(userRepository.findById(9)).thenReturn(reporter);

		assertThatThrownBy(() -> userService.createUser(9, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, UserType.REPORTER, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("admin");
	}

	@Test
	void createUserCannotCreateAnotherSuperAdmin() {
		User superAdmin = new User();
		superAdmin.setUserType(UserType.SUPER_ADMIN);
		when(userRepository.findById(1)).thenReturn(superAdmin);

		assertThatThrownBy(() -> userService.createUser(1, FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, UserType.SUPER_ADMIN, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("super admin");
	}

	// Department.id is JPA-generated with no public setter — reflection is the established
	// workaround for giving a test Department a distinct id without a real database.
	private void setDepartmentId(Department department, int id) throws Exception {
		java.lang.reflect.Field field = Department.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(department, id);
	}
}
