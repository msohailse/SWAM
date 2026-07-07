package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.CommentRepositoryPort;
import com.msohailse.app.incident.application.port.out.IncidentEventPublisherPort;
import com.msohailse.app.incident.application.port.out.IncidentRepositoryPort;
import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import com.msohailse.app.incident.domain.Tag;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
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
	IncidentEventPublisherPort eventPublisher;

	@Transactional
	public Incident create(String title, String description, Severity severity, String tagTitle, int reportedByUserId) {
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

	public List<Incident> findAll() {
		return incidentRepository.findAll();
	}

	public List<Incident> findByUser(int userId) {
		User user = userRepository.findById(userId);
		if (user == null) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
		return incidentRepository.findByUser(user);
	}

	@Transactional
	public Incident update(int id, String title, String description, Severity severity) {
		Incident incident = incidentRepository.findById(id);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + id);
		}
		incident.setTitle(title);
		incident.setDescription(description);
		incident.setSeverity(severity);
		incidentRepository.update(incident);
		return incident;
	}

	@Transactional
	public Incident close(int id, int actingUserId, String commentText) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || actingUser.getUserType() != UserType.ADMIN) {
			throw new IllegalArgumentException("Only an admin can close an incident");
		}
		Incident incident = incidentRepository.findById(id);
		if (incident == null) {
			throw new IllegalArgumentException("Incident not found: " + id);
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
