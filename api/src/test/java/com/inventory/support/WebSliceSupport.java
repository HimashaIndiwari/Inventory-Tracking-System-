package com.inventory.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.inventory.entity.Role;
import com.inventory.security.AuthenticatedUser;
import com.inventory.security.ProblemDetailWriter;
import com.inventory.security.SecurityConfig;
import com.inventory.security.VerifiedToken;
import com.inventory.service.UserProvisioningService;

/**
 * Base for @WebMvcTest slice tests. Uses the real SecurityConfig and the fake token verifier. Provisioning is
 * mocked so that "user-token" resolves to USER and "admin-token" to ADMIN. Firebase initialisation is disabled
 * (app.firebase.enabled=false), so no Firebase connection is made. The slices do not include JPA or DataSource
 * auto-configuration. Subclasses declare @WebMvcTest(controllers = ...) and mock the services their controllers need.
 */
@Import({SecurityConfig.class, FakeFirebaseTokenVerifier.class, ProblemDetailWriter.class})
@TestPropertySource(properties = "app.firebase.enabled=false")
public abstract class WebSliceSupport {

    protected static final String USER = "Bearer user-token";
    protected static final String ADMIN = "Bearer admin-token";

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected UserProvisioningService provisioning;

    @BeforeEach
    void stubProvisioning() {
        when(provisioning.resolve(any(VerifiedToken.class))).thenAnswer(inv -> {
            VerifiedToken token = inv.getArgument(0);
            Role role = "u-admin".equals(token.uid()) ? Role.ADMIN : Role.USER;
            long id = role == Role.ADMIN ? 2L : 1L;
            return new AuthenticatedUser(id, token.uid(), token.email(), token.name(), role);
        });
    }
}
