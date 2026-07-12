package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.application.service.TagService;
import com.msohailse.app.incident.domain.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagResource {

	@Inject
	TagService tagService;

	public record TagRequest(int actingUserId, String tagTitle, String tagDescription) {}

	@GET
	public List<Tag> findAll() {
		return tagService.findAll();
	}

	@GET
	@Path("/{id}")
	public Tag findById(@PathParam("id") int id) {
		return tagService.findById(id);
	}

	@POST
	public Tag create(TagRequest request) {
		return tagService.create(request.actingUserId(), request.tagTitle(), request.tagDescription());
	}

	@PUT
	@Path("/{id}")
	public Tag update(@PathParam("id") int id, TagRequest request) {
		return tagService.update(request.actingUserId(), id, request.tagTitle(), request.tagDescription());
	}

	@DELETE
	@Path("/{id}")
	public void delete(@PathParam("id") int id, @QueryParam("actingUserId") int actingUserId) {
		tagService.delete(actingUserId, id);
	}
}
