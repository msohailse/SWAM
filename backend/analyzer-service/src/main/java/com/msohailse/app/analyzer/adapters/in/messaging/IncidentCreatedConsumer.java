package com.msohailse.app.analyzer.adapters.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msohailse.app.analyzer.application.DuplicateDetector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class IncidentCreatedConsumer {

	@Inject
	DuplicateDetector duplicateDetector;

	@Inject
	ObjectMapper objectMapper;

	@Incoming("incident-created")
	public void consume(String json) throws Exception {
		JsonNode event = objectMapper.readTree(json);
		duplicateDetector.checkForDuplicate(
				event.get("incidentId").asInt(),
				event.get("title").asText(),
				event.hasNonNull("description") ? event.get("description").asText() : null);
	}
}
