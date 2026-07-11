package com.msohailse.app.incident.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DepartmentTest {

    @Test
    public void testValidName() {
        Department d = new Department();
        d.setName("IT Support");
        assertEquals("IT Support", d.getName());
    }

    @Test
    public void testInvalidName() {
        Department d = new Department();
        assertThrows(IllegalArgumentException.class, () -> d.setName(null));
        assertThrows(IllegalArgumentException.class, () -> d.setName("   "));
    }
}
