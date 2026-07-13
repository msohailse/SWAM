package com.msohailse.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msohailse.analyzer.domain.Incident;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class DuplicateDetector {

	private static final Logger LOG = Logger.getLogger(DuplicateDetector.class.getName());
	private static final double SIMILARITY_THRESHOLD = 0.4;

	// Reported over Kafka, not REST — analyzer stays a pure listen-and-report service with
	// no direct knowledge of backend's HTTP API. backend consumes this and calls the exact
	// same IncidentService.markDuplicate(...) the old REST endpoint called.
	public record AnalysisCompletedEvent(int incidentId, int duplicatedIncidentId) {}

	@Inject
	EntityManager em;

	@Inject
	@Channel("incident-analysis-completed")
	Emitter<String> emitter;

	@Inject
	ObjectMapper objectMapper;

	// pg_trgm isn't created by Hibernate's schema generation (that only knows about
	// tables/columns from @Entity classes), so the Analyzer — the service that actually
	// needs it — creates it once at startup if missing.
	@Transactional
	void onStart(@Observes StartupEvent event) {
		em.createNativeQuery("CREATE EXTENSION IF NOT EXISTS pg_trgm").executeUpdate();
	}

	@Transactional
	public void checkForDuplicate(int incidentId, String title, String description) {
		String combinedText = title + " " + (description == null ? "" : description);

		List<Object[]> matches = em.createNativeQuery(
				"SELECT id, title FROM incidents "
						+ "WHERE id != :id AND isclosed = false "
						+ "AND similarity(title || ' ' || coalesce(description, ''), :text) > :threshold "
						+ "ORDER BY similarity(title || ' ' || coalesce(description, ''), :text) DESC LIMIT 1")
				.setParameter("id", incidentId)
				.setParameter("text", combinedText)
				.setParameter("threshold", SIMILARITY_THRESHOLD)
				.getResultList();

		if (matches.isEmpty()) {
			LOG.info("Incident " + incidentId + ": no likely duplicate found");
			return;
		}

		Object[] match = matches.get(0);
		int matchedId = (Integer) match[0];
		String matchedTitle = (String) match[1];

		Incident incident = em.find(Incident.class, incidentId);
		if (incident == null) {
			return; // deleted in the meantime, nothing to comment on
		}

		try {
			String json = objectMapper.writeValueAsString(new AnalysisCompletedEvent(incidentId, matchedId));
			emitter.send(json);
			LOG.info("Incident " + incidentId + ": flagged as likely duplicate of #" + matchedId + " (\"" + matchedTitle + "\")");
		} catch (Exception e) {
			// Don't let a publish failure escape uncaught — that would propagate out of this
			// @Incoming Kafka consumer and permanently stop consumption of the whole topic
			// (SmallRye's default failure-strategy is "fail"). Log and move on instead; the
			// next incident-created event still gets processed normally.
			LOG.severe("Incident " + incidentId + ": failed to publish analysis-completed event for duplicate of #" + matchedId + ": " + e.getMessage());
		}
	}
}
