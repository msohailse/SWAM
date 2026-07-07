package com.msohailse.app.analyzer.application;

import com.msohailse.app.analyzer.domain.Comment;
import com.msohailse.app.analyzer.domain.Incident;
import com.msohailse.app.analyzer.domain.User;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class DuplicateDetector {

	private static final Logger LOG = Logger.getLogger(DuplicateDetector.class.getName());
	private static final double TRGM_THRESHOLD = 0.4;
	// pgvector's <=> is cosine DISTANCE (0 = identical, 2 = opposite), not similarity —
	// smaller is more similar. 0.45 is a first guess (calibrated against one manually
	// verified paraphrase pair at distance 0.393), not empirically tuned — see CLAUDE.md.
	private static final double COSINE_DISTANCE_THRESHOLD = 0.45;
	private static final String SYSTEM_USER_EMAIL = "system@analyzer.local";

	@Inject
	EntityManager em;

	@Inject
	EmbeddingModel embeddingModel;

	// pg_trgm/pgvector aren't created by Hibernate's schema generation (that only knows
	// about tables/columns from @Entity classes), so the Analyzer — the service that
	// actually needs them — creates them once at startup if missing.
	@Transactional
	void onStart(@Observes StartupEvent event) {
		em.createNativeQuery("CREATE EXTENSION IF NOT EXISTS pg_trgm").executeUpdate();
		em.createNativeQuery("CREATE EXTENSION IF NOT EXISTS vector").executeUpdate();
		em.createNativeQuery(
				"CREATE TABLE IF NOT EXISTS incident_embeddings ("
						+ "incident_id integer PRIMARY KEY REFERENCES incidents(id), "
						+ "embedding vector(384))")
				.executeUpdate();
	}

	@Transactional
	public void checkForDuplicate(int incidentId, String title, String description) {
		String combinedText = title + " " + (description == null ? "" : description);

		Incident incident = em.find(Incident.class, incidentId);
		if (incident == null) {
			return; // deleted in the meantime
		}

		Optional<Match> match = findTrgmDuplicate(incidentId, combinedText);
		if (match.isEmpty()) {
			match = findSemanticDuplicate(incidentId, combinedText);
		}

		// Store this incident's embedding regardless of the outcome, so later incidents
		// can be compared against it too.
		storeEmbedding(incidentId, combinedText);

		if (match.isEmpty()) {
			LOG.info("Incident " + incidentId + ": no likely duplicate found");
			return;
		}

		Comment comment = new Comment();
		comment.setText("Possible duplicate of incident #" + match.get().id() + " (\"" + match.get().title() + "\") — " + match.get().stage());
		comment.setIncident(incident);
		comment.setAuthor(findOrCreateSystemUser());
		em.persist(comment);

		LOG.info("Incident " + incidentId + ": flagged as likely duplicate of #" + match.get().id() + " via " + match.get().stage());
	}

	private record Match(int id, String title, String stage) {}

	private Optional<Match> findTrgmDuplicate(int incidentId, String combinedText) {
		List<Object[]> matches = em.createNativeQuery(
				"SELECT id, title FROM incidents "
						+ "WHERE id != :id AND isclosed = false "
						+ "AND similarity(title || ' ' || coalesce(description, ''), :text) > :threshold "
						+ "ORDER BY similarity(title || ' ' || coalesce(description, ''), :text) DESC LIMIT 1")
				.setParameter("id", incidentId)
				.setParameter("text", combinedText)
				.setParameter("threshold", TRGM_THRESHOLD)
				.getResultList();

		if (matches.isEmpty()) {
			return Optional.empty();
		}
		Object[] row = matches.get(0);
		return Optional.of(new Match((Integer) row[0], (String) row[1], "Stage 1 (lexical match)"));
	}

	private Optional<Match> findSemanticDuplicate(int incidentId, String combinedText) {
		String vectorLiteral = toVectorLiteral(embeddingModel.embed(combinedText).content());

		List<Object[]> matches = em.createNativeQuery(
				"SELECT i.id, i.title FROM incident_embeddings e "
						+ "JOIN incidents i ON i.id = e.incident_id "
						+ "WHERE e.incident_id != :id AND i.isclosed = false "
						+ "AND (e.embedding <=> CAST('" + vectorLiteral + "' AS vector)) < :threshold "
						+ "ORDER BY e.embedding <=> CAST('" + vectorLiteral + "' AS vector) LIMIT 1")
				.setParameter("id", incidentId)
				.setParameter("threshold", COSINE_DISTANCE_THRESHOLD)
				.getResultList();

		if (matches.isEmpty()) {
			return Optional.empty();
		}
		Object[] row = matches.get(0);
		return Optional.of(new Match((Integer) row[0], (String) row[1], "Stage 2 (semantic match)"));
	}

	private void storeEmbedding(int incidentId, String combinedText) {
		Embedding embedding = embeddingModel.embed(combinedText).content();
		String vectorLiteral = toVectorLiteral(embedding);
		em.createNativeQuery(
				"INSERT INTO incident_embeddings (incident_id, embedding) VALUES (:id, CAST('" + vectorLiteral + "' AS vector)) "
						+ "ON CONFLICT (incident_id) DO UPDATE SET embedding = EXCLUDED.embedding")
				.setParameter("id", incidentId)
				.executeUpdate();
	}

	private String toVectorLiteral(Embedding embedding) {
		StringBuilder sb = new StringBuilder("[");
		float[] vector = embedding.vector();
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(vector[i]);
		}
		return sb.append(']').toString();
	}

	private User findOrCreateSystemUser() {
		try {
			return em.createQuery("select u from User u where u.email = :email", User.class)
					.setParameter("email", SYSTEM_USER_EMAIL)
					.getSingleResult();
		} catch (NoResultException e) {
			User systemUser = new User();
			systemUser.setFirstName("Duplicate");
			systemUser.setLastName("Analyzer");
			systemUser.setEmail(SYSTEM_USER_EMAIL);
			systemUser.setPassword("not-a-real-login");
			systemUser.setUserType("ADMIN");
			em.persist(systemUser);
			return systemUser;
		}
	}
}
