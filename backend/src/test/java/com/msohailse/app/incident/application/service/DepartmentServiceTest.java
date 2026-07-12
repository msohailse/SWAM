package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DepartmentServiceTest {

    @Inject
    DepartmentService service;

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    UserTransaction userTransaction;

    private int adminId;
    private int reporterId;

    @BeforeEach
    void setUp() throws Exception {
        userTransaction.begin();
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("dept-admin-" + System.nanoTime() + "@example.com");
        admin.setPassword("SecurePass1");
        admin.setUserType(UserType.SUPER_ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User reporter = new User();
        reporter.setFirstName("Reporter");
        reporter.setLastName("User");
        reporter.setEmail("dept-reporter-" + System.nanoTime() + "@example.com");
        reporter.setPassword("SecurePass1");
        userRepository.save(reporter);
        reporterId = reporter.getId();
        userTransaction.commit();
    }

    @Test
    public void testCreateDuplicate() {
        service.create(adminId, "HR-" + System.nanoTime(), "Human Resources");
        String name = "HR-Dup-" + System.nanoTime();
        service.create(adminId, name, "Human Resources");
        assertThrows(IllegalArgumentException.class, () -> service.create(adminId, name, "Duplicate"));
    }

    @Test
    public void testCreateByNonAdminThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(reporterId, "Finance-" + System.nanoTime(), "Finance dept"));
    }
}
