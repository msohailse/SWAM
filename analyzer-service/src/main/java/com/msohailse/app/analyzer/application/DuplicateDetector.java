package com.msohailse.app.analyzer.application;

import com.msohailse.app.analyzer.domain.Incident;
import com.msohailse.app.analyzer.adapters.out.rest.BackendApiClient;
import com.msohailse.app.analyzer.adapters.out.rest.BackendApiClient.MarkDuplicateRequest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class DuplicateDetector {

	private static final Logger LOG = Logger.getLogger(DuplicateDetector.class.getName());
	private static final double SIMILARITY_THRESHOLD = 0.4;
	private static final String SYSTEM_USER_EMAIL = "system@analyzer.local";

	@Inject
	EntityManager em;

	@Inject
	@RestClient
	BackendApiClient backendApiClient;

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

		backendApiClient.markDuplicate(incidentId, new MarkDuplicateRequest(matchedId));

		LOG.info("Incident " + incidentId + ": flagged as likely duplicate of #" + matchedId);
	}
}
