package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.application.service.IncidentService;
import com.msohailse.app.incident.domain.Comment;
import com.msohailse.app.incident.domain.Incident;
import com.msohailse.app.incident.domain.Severity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/incidents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IncidentResource {

	@Inject
	IncidentService incidentService;

	public record CreateIncidentRequest(String title, String description, Severity severity, String tagTitle, int reportedByUserId) {}
	public record UpdateIncidentRequest(String title, String description, Severity severity) {}
	public record CloseIncidentRequest(int actingUserId, String commentText) {}
	public record AddCommentRequest(int authorUserId, String text) {}

	// CQRS-lite: GET /incidents?tag=&severity=&status= — all three are optional, plain
	// GET /incidents still returns everything, unfiltered, exactly as before.
	@GET
	public List<Incident> findAll(@QueryParam("tag") String tag, @QueryParam("severity") Severity severity,
			@QueryParam("status") String status) {
		if (tag == null && severity == null && status == null) {
			return incidentService.findAll();
		}
		return incidentService.findFiltered(tag, severity, status);
	}

	@GET
	@Path("/{id}")
	public Incident findById(@PathParam("id") int id) {
		return incidentService.findById(id);
	}

	@GET
	@Path("/user/{userId}")
	public List<Incident> findByUser(@PathParam("userId") int userId) {
		return incidentService.findByUser(userId);
	}

	@POST
	public Incident create(CreateIncidentRequest request) {
		return incidentService.create(request.title(), request.description(), request.severity(),
				request.tagTitle(), request.reportedByUserId());
	}

	@PUT
	@Path("/{id}")
	public Incident update(@PathParam("id") int id, UpdateIncidentRequest request) {
		return incidentService.update(id, request.title(), request.description(), request.severity());
	}

	@PATCH
	@Path("/{id}/close")
	public Incident close(@PathParam("id") int id, CloseIncidentRequest request) {
		return incidentService.close(id, request.actingUserId(), request.commentText());
	}

	@GET
	@Path("/{id}/comments")
	public List<Comment> findComments(@PathParam("id") int id) {
		return incidentService.findComments(id);
	}

	@POST
	@Path("/{id}/comments")
	public Comment addComment(@PathParam("id") int id, AddCommentRequest request) {
		return incidentService.addComment(id, request.authorUserId(), request.text());
	}

	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") int id) {
		incidentService.delete(id);
	}
}
