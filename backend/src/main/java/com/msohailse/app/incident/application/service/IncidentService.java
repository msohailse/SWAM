package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.CommentRepositoryPort;
import com.msohailse.app.incident.application.port.out.IncidentEventPublisherPort;
import com.msohailse.app.incident.application.port.out.IncidentRepositoryPort;
import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Department;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.Tag;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class IncidentService {

	@Inject
	IncidentRepositoryPort incidentRepository;

	@Inject
	TagRepositoryPort tagRepository;

	@Inject
	UserRepositoryPort userRepository;

	@Inject
	CommentRepositoryPort commentRepository;

	@Inject
	DepartmentRepositoryPort departmentRepository;

	@Inject
	IncidentEventPublisherPort eventPublisher;

	@Transactional
	public Incident create(String title, String description, Severity severity, String tagTitle, int reportedByUserId, Integer assignedDepartmentId) {
		User reportedBy = userRepository.findById(reportedByUserId);
		if (reportedBy == null) {
			throw new IllegalArgumentException("User not found: " + reportedByUserId);
		}
		Tag tag = tagRepository.findByTitle(tagTitle);
		if (tag == null) {
			tag = new Tag();
			tag.setTagTitle(tagTitle);
			tagRepository.save(tag);
		}
		Incident incident = new Incident();
		incident.setTitle(title);
		incident.setDescription(description);
		incident.setSeverity(severity);
		incident.setReportedBy(reportedBy);
		incident.setTag(tag);
		if (assignedDepartmentId != null) {
			Department dept = departmentRepository.findById(assignedDepartmentId);
			if (dept == null) {
				throw new IllegalArgumentException("Department not found: " + assignedDepartmentId);
			}
			incident.setAssignedDepartment(dept);
		}
		incidentRepository.save(incident);

		// Fire-and-forget: the Analyzer service picks this up asynchronously to check for
		// duplicates. Creation itself stays synchronous/immediate — this doesn't change the
		// already-working create flow, it just adds a side effect after the save.
		eventPublisher.publishIncidentCreated(incident.getId(), title, description, tagTitle);

		return incident;
	}

	public Incident findById(int id) {
		return incidentRepository.findById(id);
	}

	public List<Incident> findByUser(int userId) {
		User user = userRepository.findById(userId);
		if (user == null) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
		return incidentRepository.findByUser(user);
	}

	// CQRS-lite: a read-only query endpoint (GET /incidents?tag=&severity=&status=) that
	// is separate from the create/update/close write path above. It doesn't introduce a
	// separate read model/projection table (that would be full CQRS) — it just lets the
	// read side filter flexibly without touching the write side at all. Every role uses
	// this same query — the only difference is which extra scope (if any) gets layered on
	// top of the tag/severity/status filters the caller asked for.
	//
	// actingUserId drives that scope: a super admin sees everything, a department admin
	// only ever sees their own department's incidents, and anyone else (a reporter, or an
	// admin whose grant expired) is scoped to just the incidents they themselves reported —
	// the scope always comes from the caller's own User record, never from a client-supplied
	// filter, so it can't be widened by the request itself.
	public List<Incident> findFiltered(String tagTitle, Severity severity, String status, Integer actingUserId) {
		Boolean closed = parseStatus(status);
		ListScope scope = resolveListScope(actingUserId);
		return incidentRepository.findFiltered(tagTitle, severity, closed, scope.department(), scope.reportedBy());
	}

	private record ListScope(Department department, User reportedBy) {}

	private static final ListScope UNSCOPED = new ListScope(null, null);

	// null actingUserId -> unscoped (used by callers that aren't listing on behalf of a
	// specific user at all). A super admin also gets unscoped -> sees everything. An
	// active (non-expired) department admin gets scoped to their own Department. Anyone
	// else (a reporter, or an admin whose grant expired) gets scoped to their own reports
	// instead — same list a plain reporter always saw, just now composable with filters.
	private ListScope resolveListScope(Integer actingUserId) {
		if (actingUserId == null) {
			return UNSCOPED;
		}
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null) {
			throw new IllegalArgumentException("User not found: " + actingUserId);
		}
		if (actingUser.isSuperAdmin()) {
			return UNSCOPED;
		}
		if (actingUser.getUserType() == UserType.ADMIN && actingUser.isActiveAdmin()) {
			return new ListScope(actingUser.getDepartment(), null);
		}
		return new ListScope(null, actingUser);
	}

	// status is a query string, so it comes in as "open"/"closed", not a boolean —
	// translate it here, once, instead of pushing string-parsing into the repository.
	private Boolean parseStatus(String status) {
		if (status == null) {
			return null;
		}
		if (status.equalsIgnoreCase("open")) {
			return false;
		}
		if (status.equalsIgnoreCase("closed")) {
			return true;
		}
		throw new IllegalArgumentException("status must be 'open' or 'closed', got: " + status);
	}

	@Transactional
	public Incident update(int id, int actingUserId, String title, String description, Severity severity, Integer assignedDepartmentId) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null) {
			throw new IllegalArgumentException("User not found: " + actingUserId);
		}
		Incident incident = incidentRepository.findById(id);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + id);
		}

		// Only a super admin may (re)assign a department — a plain resend of the incident's
		// current department (which the UI always does for anyone who isn't editing it) is
		// not a change and never blocked.
		Integer currentDepartmentId = incident.getAssignedDepartment() == null ? null : incident.getAssignedDepartment().getId();
		boolean departmentChanging = !java.util.Objects.equals(currentDepartmentId, assignedDepartmentId);
		if (departmentChanging && !actingUser.isSuperAdmin()) {
			throw new IllegalArgumentException("Only a super admin can assign a department");
		}

		incident.setTitle(title);
		incident.setDescription(description);
		incident.setSeverity(severity);
		if (assignedDepartmentId != null) {
			Department dept = departmentRepository.findById(assignedDepartmentId);
			if (dept == null) {
				throw new IllegalArgumentException("Department not found: " + assignedDepartmentId);
			}
			incident.setAssignedDepartment(dept);
		} else {
			incident.setAssignedDepartment(null);
		}
		incidentRepository.update(incident);
		return incident;
	}

	@Transactional
	public Incident markDuplicate(int id, int duplicatedIncidentId) {
		if (id == duplicatedIncidentId) {
			throw new IllegalArgumentException("An incident cannot be a duplicate of itself");
		}
		Incident incident = incidentRepository.findById(id);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + id);
		}
		Incident duplicated = incidentRepository.findById(duplicatedIncidentId);
		if (duplicated == null) {
			throw new IllegalArgumentException("Duplicated incident not found: " + duplicatedIncidentId);
		}
		incident.setIsDuplicate(true);
		incident.setDuplicatedIncidentId(duplicatedIncidentId);
		incidentRepository.update(incident);

		// analyzer-service reports duplicates over REST rather than writing to Postgres
		// itself (it owns no CRUD/domain code, see analyzer-service's DuplicateDetector) —
		// so the system comment it used to post directly is created here instead, in the
		// same transaction as the flag update, using the one place that already owns
		// Comment/User.
		saveComment(incident, findOrCreateSystemUser(),
				"Possible duplicate of incident #" + duplicatedIncidentId + " (\"" + duplicated.getTitle() + "\")");

		return incident;
	}

	private static final String SYSTEM_USER_EMAIL = "system@analyzer.local";

	private User findOrCreateSystemUser() {
		User systemUser = userRepository.findByEmail(SYSTEM_USER_EMAIL);
		if (systemUser != null) {
			return systemUser;
		}
		systemUser = new User();
		systemUser.setFirstName("Duplicate");
		systemUser.setLastName("Analyzer");
		systemUser.setEmail(SYSTEM_USER_EMAIL);
		systemUser.setPassword("NotARealLogin1");
		systemUser.setUserType(UserType.ADMIN);
		userRepository.save(systemUser);
		return systemUser;
	}

	@Transactional
	public Incident close(int id, int actingUserId, String commentText, Integer assignedDepartmentId) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || !actingUser.isActiveAdmin()) {
			throw new IllegalArgumentException("Only an admin can close an incident");
		}
		Incident incident = incidentRepository.findById(id);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + id);
		}

		// Same "only a real change needs a super admin" rule as update() — the close modal
		// always resends the incident's current department for a department admin (who never
		// sees the reassignment control at all), so that must stay a no-op, not a rejection.
		Integer currentDepartmentId = incident.getAssignedDepartment() == null ? null : incident.getAssignedDepartment().getId();
		boolean departmentChanging = assignedDepartmentId != null && !assignedDepartmentId.equals(currentDepartmentId);
		if (departmentChanging) {
			if (!actingUser.isSuperAdmin()) {
				throw new IllegalArgumentException("Only a super admin can assign a department");
			}
			Department dept = departmentRepository.findById(assignedDepartmentId);
			if (dept == null) {
				throw new IllegalArgumentException("Department not found: " + assignedDepartmentId);
			}
			incident.setAssignedDepartment(dept);
		}

		if (incident.getAssignedDepartment() == null) {
			throw new IllegalArgumentException("Cannot close an incident without an assigned department");
		}

		// A department admin (already confirmed active above) may only close incidents
		// already assigned to their own department — the super admin has no such restriction.
		if (!actingUser.isSuperAdmin() && incident.getAssignedDepartment().getId() != actingUser.getDepartment().getId()) {
			throw new IllegalArgumentException("You can only close incidents assigned to your own department");
		}

		incident.setIsClosed(true);
		incidentRepository.update(incident);

		saveComment(incident, actingUser, commentText);
		return incident;
	}

	// Both the reporter and an admin can post to an incident's comment thread — e.g. the
	// reporter replying to the admin's closing comment. Only closing itself is admin-only.
	@Transactional
	public Comment addComment(int incidentId, int authorUserId, String text) {
		Incident incident = incidentRepository.findById(incidentId);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + incidentId);
		}
		User author = userRepository.findById(authorUserId);
		if (author == null) {
			throw new IllegalArgumentException("User not found: " + authorUserId);
		}
		return saveComment(incident, author, text);
	}

	private Comment saveComment(Incident incident, User author, String text) {
		Comment comment = new Comment();
		comment.setText(text);
		comment.setIncident(incident);
		comment.setAuthor(author);
		commentRepository.save(comment);
		return comment;
	}

	public List<Comment> findComments(int incidentId) {
		Incident incident = incidentRepository.findById(incidentId);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + incidentId);
		}
		return commentRepository.findByIncident(incident);
	}

	@Transactional
	public void delete(int id) {
		incidentRepository.delete(id);
	}
}
