package com.msohailse.app.incident.adapters.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msohailse.app.incident.application.port.out.IncidentEventPublisherPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class KafkaIncidentEventPublisher implements IncidentEventPublisherPort {

	@Inject
	@Channel("incident-created")
	Emitter<String> emitter;

	@Inject
	ObjectMapper objectMapper;

	public record IncidentCreatedEvent(int incidentId, String title, String description, String tagTitle) {}

	@Override
	public void publishIncidentCreated(int incidentId, String title, String description, String tagTitle) {
		try {
			String json = objectMapper.writeValueAsString(new IncidentCreatedEvent(incidentId, title, description, tagTitle));
			emitter.send(json);
		} catch (Exception e) {
			throw new RuntimeException("Failed to publish incident-created event", e);
		}
	}
}
