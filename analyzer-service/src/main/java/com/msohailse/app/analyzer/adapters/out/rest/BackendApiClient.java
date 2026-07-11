package com.msohailse.app.analyzer.adapters.out.rest;

import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/incidents")
@RegisterRestClient(configKey = "backend-api")
public interface BackendApiClient {

	@PATCH
	@Path("/{id}/mark-duplicate")
	void markDuplicate(@PathParam("id") int id, MarkDuplicateRequest request);

	record MarkDuplicateRequest(int duplicatedIncidentId) {}
}
