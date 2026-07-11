package com.msohailse.app.incident.adapters.out.persistence;

import com.msohailse.app.incident.application.port.out.DepartmentRepositoryPort;
import com.msohailse.app.incident.domain.Department;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DepartmentPostgresRepositoryTest {

    @Inject
    DepartmentRepositoryPort repo;

    @Test
    @Transactional
    public void testSaveAndFind() {
        Department d = new Department();
        d.setName("DevOps");
        d.setDescription("Infrastructure team");
        repo.save(d);

        assertNotNull(repo.findById(d.getId()));
        assertNotNull(repo.findByName("DevOps"));
        assertTrue(repo.findAll().size() > 0);
    }
}
