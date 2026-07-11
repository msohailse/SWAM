package com.msohailse.app.analyzer;

import com.msohailse.app.analyzer.application.DuplicateDetector;
import com.msohailse.app.analyzer.adapters.out.rest.BackendApiClient;
import com.msohailse.app.analyzer.adapters.out.rest.BackendApiClient.MarkDuplicateRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class DuplicateDetectorTest {

    @Inject
    DuplicateDetector duplicateDetector;

    @Inject
    EntityManager em;

    @InjectMock
    @RestClient
    BackendApiClient backendApiClient;

    @BeforeEach
    @Transactional
    public void setup() {
        // The analyzer relies on api-service to manage the schema, so we create the minimal
        // table needed for testing pg_trgm locally.
        em.createNativeQuery("CREATE TABLE IF NOT EXISTS incidents (id INT PRIMARY KEY, title VARCHAR(200), description TEXT, isclosed BOOLEAN)").executeUpdate();
        em.createNativeQuery("DELETE FROM incidents").executeUpdate();
    }

    @Test
    @Transactional
    public void testDuplicateDetection() {
        // Setup existing incident in the database
        em.createNativeQuery("INSERT INTO incidents (id, title, description, isclosed) VALUES (1, 'The database server is completely down', 'We cannot connect to production DB', false)").executeUpdate();

        // Emulate consuming a new incident event that is a duplicate
        int newIncidentId = 2;
        em.createNativeQuery("INSERT INTO incidents (id, title, description, isclosed) VALUES (2, 'Production DB server is down', 'Cannot connect to database', false)").executeUpdate();

        duplicateDetector.checkForDuplicate(newIncidentId, "Production DB server is down", "Cannot connect to database");

        // Verify the REST client was called to mark the new incident as a duplicate of the existing one
        Mockito.verify(backendApiClient).markDuplicate(Mockito.eq(newIncidentId), Mockito.eq(new MarkDuplicateRequest(1)));
    }
}
