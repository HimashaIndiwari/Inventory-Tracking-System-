package com.inventory.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.inventory.controller.AuthController;
import com.inventory.support.WebSliceSupport;

/**
 * Security rules only: authentication (401), role-based denial (403), the security filter's error responses
 * (503, 500) and CORS. Only AuthController is loaded here. Security decides before dispatch, so denials for the
 * catalog and stock endpoints are checked without loading their controllers. Success and validation statuses are
 * tested in the controller test classes.
 */
@WebMvcTest(controllers = AuthController.class)
class SecurityRulesTest extends WebSliceSupport {

    /** An endpoint that writes data, with the exact method and path that the controllers map. */
    private record Call(HttpMethod method, String path) {
    }

    /** Catalog writes, as mapped by ProductController, CategoryController and SupplierController. */
    static List<Call> catalogWrites() {
        return List.of(
                new Call(HttpMethod.POST, "/api/products"),
                new Call(HttpMethod.PUT, "/api/products/1"),
                new Call(HttpMethod.DELETE, "/api/products/1"),
                new Call(HttpMethod.POST, "/api/categories"),
                new Call(HttpMethod.PUT, "/api/categories/1"),
                new Call(HttpMethod.DELETE, "/api/categories/1"),
                new Call(HttpMethod.POST, "/api/suppliers"),
                new Call(HttpMethod.PUT, "/api/suppliers/1"),
                new Call(HttpMethod.DELETE, "/api/suppliers/1"));
    }

    /** Stock writes, as mapped by StockController. */
    static List<Call> stockWrites() {
        return List.of(
                new Call(HttpMethod.POST, "/api/stock/in"),
                new Call(HttpMethod.POST, "/api/stock/out"),
                new Call(HttpMethod.POST, "/api/stock/adjust"));
    }

    private static MockHttpServletRequestBuilder build(Call call, String authorization) {
        MockHttpServletRequestBuilder builder = request(call.method(), call.path())
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        return authorization == null ? builder : builder.header(HttpHeaders.AUTHORIZATION, authorization);
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // The fake verifier maps revoked, disabled and expired tokens to the same outcome (its default branch).
    // The real mapping to InvalidTokenException is tested in FirebaseAdminTokenVerifierTest.
    @Test
    void revokedTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer revoked-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void disabledTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer disabled-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firebaseOutageIs503() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer firebase-down-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTH_SERVICE_UNAVAILABLE"));
    }

    @Test
    void unexpectedVerifierFailureIs500() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer boom-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @ParameterizedTest(name = "no token: {0}")
    @MethodSource("catalogWrites")
    void catalogWriteWithoutTokenIs401(Call call) throws Exception {
        mvc.perform(build(call, null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest(name = "USER denied: {0}")
    @MethodSource("catalogWrites")
    void userCannotWriteCatalog(Call call) throws Exception {
        mvc.perform(build(call, USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest(name = "no token: {0}")
    @MethodSource("stockWrites")
    void stockWriteWithoutTokenIs401(Call call) throws Exception {
        mvc.perform(build(call, null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void userCannotAdjustStock() throws Exception {
        mvc.perform(build(new Call(HttpMethod.POST, "/api/stock/adjust"), USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unlistedPathIsDenied() throws Exception {
        mvc.perform(get("/internal/anything").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsPreflightAllowsConfiguredOriginOnly() throws Exception {
        mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
