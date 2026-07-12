package com.msohailse.app.incident.adapters.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msohailse.app.incident.application.service.IncidentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class AnalysisCompletedConsumer {

	private static final Logger LOG = Logger.getLogger(AnalysisCompletedConsumer.class.getName());

	@Inject
	IncidentService incidentService;

	@Inject
	ObjectMapper objectMapper;

	@Incoming("incident-analysis-completed")
	public void consume(String json) {
		try {
			JsonNode event = objectMapper.readTree(json);
			incidentService.markDuplicate(event.get("incidentId").asInt(), event.get("duplicatedIncidentId").asInt());
		} catch (Exception e) {
			// Same reasoning as analyzer-service's own consumer: don't let a bad/stale event
			// (e.g. one of the two incidents got deleted in the meantime) escape uncaught and
			// wedge this topic's consumption under Kafka's default failure strategy.
			LOG.severe("Failed to process analysis-completed event: " + json + " (" + e.getMessage() + ")");
		}
	}
}
