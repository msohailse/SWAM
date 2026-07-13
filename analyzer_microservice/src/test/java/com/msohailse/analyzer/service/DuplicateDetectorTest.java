package com.msohailse.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msohailse.analyzer.service.DuplicateDetector;
import com.msohailse.analyzer.service.DuplicateDetector.AnalysisCompletedEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class DuplicateDetectorTest {

    @Inject
    DuplicateDetector duplicateDetector;

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    @Transactional
    public void setup() {
        // The analyzer relies on api-service to manage the schema, so we create the minimal
        // table needed for testing pg_trgm locally.
        em.createNativeQuery("CREATE TABLE IF NOT EXISTS incidents (id INT PRIMARY KEY, title VARCHAR(200), description TEXT, isclosed BOOLEAN)").executeUpdate();
        em.createNativeQuery("DELETE FROM incidents").executeUpdate();
        connector.sink("incident-analysis-completed").clear();
    }

    @Test
    @Transactional
    public void testDuplicateDetection() throws Exception {
        // Setup existing incident in the database
        em.createNativeQuery("INSERT INTO incidents (id, title, description, isclosed) VALUES (1, 'The database server is completely down', 'We cannot connect to production DB', false)").executeUpdate();

        // Emulate consuming a new incident event that is a duplicate
        int newIncidentId = 2;
        em.createNativeQuery("INSERT INTO incidents (id, title, description, isclosed) VALUES (2, 'Production DB server is down', 'Cannot connect to database', false)").executeUpdate();

        duplicateDetector.checkForDuplicate(newIncidentId, "Production DB server is down", "Cannot connect to database");

        // Verify an AnalysisCompletedEvent was published to Kafka (in-memory connector in
        // tests, see %test.mp.messaging.outgoing.incident-analysis-completed.connector) instead
        // of the old direct REST call to backend.
        InMemorySink<String> sink = connector.sink("incident-analysis-completed");
        assertThat(sink.received()).hasSize(1);
        AnalysisCompletedEvent event = objectMapper.readValue(sink.received().get(0).getPayload(), AnalysisCompletedEvent.class);
        assertThat(event.incidentId()).isEqualTo(newIncidentId);
        assertThat(event.duplicatedIncidentId()).isEqualTo(1);
    }
}
