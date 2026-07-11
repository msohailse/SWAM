package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.domain.Department;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DepartmentServiceTest {

    @Inject
    DepartmentService service;

    @Test
    public void testCreateDuplicate() {
        service.create("HR", "Human Resources");
        assertThrows(IllegalArgumentException.class, () -> service.create("HR", "Duplicate"));
    }
}
