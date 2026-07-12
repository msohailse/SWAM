package com.msohailse.app.incident.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msohailse.app.incident.application.port.out.CommentRepositoryPort;
import com.msohailse.app.incident.application.port.out.IncidentEventPublisherPort;
import com.msohailse.app.incident.application.port.out.IncidentRepositoryPort;
import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.Tag;
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

public class IncidentServiceTest {

	private static final String TITLE = "Smoke detected";
	private static final String DESCRIPTION = "Second floor near server room";
	private static final Severity SEVERITY = Severity.HIGH;
	private static final String TAG_TITLE = "fire";
	private static final int USER_ID = 1;

	@InjectMocks
	private IncidentService incidentService;

	@Mock
	private IncidentRepositoryPort incidentRepository;

	@Mock
	private TagRepositoryPort tagRepository;

	@Mock
	private UserRepositoryPort userRepository;

	@Mock
	private CommentRepositoryPort commentRepository;

	@Mock
	private DepartmentRepositoryPort departmentRepository;

	@Mock
	private IncidentEventPublisherPort eventPublisher;

	private AutoCloseable closeable;
	private User reportedBy;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
		reportedBy = new User();
		reportedBy.setFirstName("John");
		reportedBy.setLastName("Doe");
		reportedBy.setEmail("john@example.com");
		reportedBy.setPassword("SecurePass1");
		when(userRepository.findById(USER_ID)).thenReturn(reportedBy);
	}

	@AfterEach
	void tearDown() throws Exception {
		closeable.close();
	}

	@Test
	void createWhenTagExistsReusesTagWithoutSaving() {
		Tag existingTag = new Tag();
		existingTag.setTagTitle(TAG_TITLE);
		when(tagRepository.findByTitle(TAG_TITLE)).thenReturn(existingTag);

		incidentService.create(TITLE, DESCRIPTION, SEVERITY, TAG_TITLE, USER_ID, null);

		verify(tagRepository).findByTitle(TAG_TITLE);
		verify(tagRepository, never()).save(any(Tag.class));
	}

	@Test
	void createWhenTagNotFoundCreatesAndSavesTagBeforeIncident() {
		when(tagRepository.findByTitle(TAG_TITLE)).thenReturn(null);

		incidentService.create(TITLE, DESCRIPTION, SEVERITY, TAG_TITLE, USER_ID, null);

		InOrder inOrder = inOrder(tagRepository, incidentRepository);
		inOrder.verify(tagRepository).findByTitle(TAG_TITLE);
		inOrder.verify(tagRepository).save(any(Tag.class));
		inOrder.verify(incidentRepository).save(any(Incident.class));
	}

	@Test
	void createSavesIncidentWithCorrectFields() {
		Tag existingTag = new Tag();
		existingTag.setTagTitle(TAG_TITLE);
		when(tagRepository.findByTitle(TAG_TITLE)).thenReturn(existingTag);

		Incident created = incidentService.create(TITLE, DESCRIPTION, SEVERITY, TAG_TITLE, USER_ID, null);

		ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
		verify(incidentRepository).save(captor.capture());
		Incident saved = captor.getValue();
		assertThat(saved.getTitle()).isEqualTo(TITLE);
		assertThat(saved.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(saved.getSeverity()).isEqualTo(SEVERITY);
		assertThat(saved.getReportedBy()).isEqualTo(reportedBy);
		assertThat(saved.getTag()).isEqualTo(existingTag);
		assertThat(created).isSameAs(saved);
		verify(eventPublisher).publishIncidentCreated(created.getId(), TITLE, DESCRIPTION, TAG_TITLE);
	}

	@Test
	void createWhenUserNotFoundThrows() {
		when(userRepository.findById(999)).thenReturn(null);

		assertThatThrownBy(() -> incidentService.create(TITLE, DESCRIPTION, SEVERITY, TAG_TITLE, 999, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void updateChangesTitleDescriptionAndSeverity() {
		Incident existing = new Incident();
		existing.setTitle("Old title");
		existing.setDescription("Old description");
		existing.setSeverity(Severity.LOW);
		existing.setReportedBy(reportedBy);
		existing.setTag(new Tag());
		when(incidentRepository.findById(5)).thenReturn(existing);

		Incident updated = incidentService.update(5, USER_ID, "New title", "New description", Severity.HIGH, null);

		assertThat(updated.getTitle()).isEqualTo("New title");
		assertThat(updated.getDescription()).isEqualTo("New description");
		assertThat(updated.getSeverity()).isEqualTo(Severity.HIGH);
		verify(incidentRepository).update(existing);
	}

	@Test
	void updateWhenIncidentNotFoundThrows() {
		when(incidentRepository.findById(404)).thenReturn(null);

		assertThatThrownBy(() -> incidentService.update(404, USER_ID, TITLE, DESCRIPTION, SEVERITY, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void closeByAdminMarksIncidentClosed() {
		User admin = new User();
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setEmail("admin@example.com");
		admin.setPassword("SecurePass1");
		admin.setUserType(UserType.ADMIN);
		when(userRepository.findById(2)).thenReturn(admin);

		Incident existing = new Incident();
		existing.setTitle(TITLE);
		existing.setDescription(DESCRIPTION);
		existing.setSeverity(SEVERITY);
		existing.setReportedBy(reportedBy);
		existing.setTag(new Tag());
		existing.setAssignedDepartment(new com.msohailse.app.incident.domain.Department());
		when(incidentRepository.findById(5)).thenReturn(existing);

		Incident closed = incidentService.close(5, 2, "Resolved, fixed the wiring", null);

		assertThat(closed.isClosed()).isTrue();
		verify(incidentRepository).update(existing);

		ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
		verify(commentRepository).save(commentCaptor.capture());
		assertThat(commentCaptor.getValue().getText()).isEqualTo("Resolved, fixed the wiring");
		assertThat(commentCaptor.getValue().getIncident()).isEqualTo(existing);
		assertThat(commentCaptor.getValue().getAuthor()).isEqualTo(admin);
	}

	@Test
	void closeByNonAdminThrows() {
		when(userRepository.findById(USER_ID)).thenReturn(reportedBy); // REPORTER by default

		assertThatThrownBy(() -> incidentService.close(5, USER_ID, "not allowed", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("admin");
		verify(incidentRepository, never()).update(any(Incident.class));
		verify(commentRepository, never()).save(any(Comment.class));
	}

	@Test
	void closeWithoutDepartmentThrows() {
		User admin = new User();
		admin.setUserType(UserType.ADMIN);
		when(userRepository.findById(2)).thenReturn(admin);

		Incident existing = new Incident();
		when(incidentRepository.findById(5)).thenReturn(existing);

		assertThatThrownBy(() -> incidentService.close(5, 2, "Closing", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("assigned department");
	}

	@Test
	void deleteDelegatesToRepository() {
		incidentService.delete(7);

		verify(incidentRepository).delete(7);
	}

	@Test
	void addCommentAllowsTheReporterToReply() {
		Incident existing = new Incident();
		existing.setTitle(TITLE);
		existing.setDescription(DESCRIPTION);
		existing.setSeverity(SEVERITY);
		existing.setReportedBy(reportedBy);
		existing.setTag(new Tag());
		when(incidentRepository.findById(5)).thenReturn(existing);

		Comment reply = incidentService.addComment(5, USER_ID, "Thanks, confirming it's fixed now");

		assertThat(reply.getText()).isEqualTo("Thanks, confirming it's fixed now");
		assertThat(reply.getIncident()).isEqualTo(existing);
		assertThat(reply.getAuthor()).isEqualTo(reportedBy);
		verify(commentRepository).save(reply);
	}

	@Test
	void addCommentWhenIncidentNotFoundThrows() {
		when(incidentRepository.findById(404)).thenReturn(null);

		assertThatThrownBy(() -> incidentService.addComment(404, USER_ID, "hi"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
