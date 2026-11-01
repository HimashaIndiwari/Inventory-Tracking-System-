package com.inventory.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.inventory.exception.ApiFieldError;
import com.inventory.exception.ErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the RFC 9457 problem body used by every error response: type, title, status, detail, instance,
 * plus "code" and "fieldErrors" (always present). Shared by the MVC exception handler and the security
 * filter/handlers so both produce the same JSON.
 */
@Component
public class ProblemDetailWriter {

    private final JsonMapper jsonMapper;

    public ProblemDetailWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Map<String, Object> body(HttpStatus status, ErrorCode code, String detail, String instance,
            List<ApiFieldError> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", instance);
        body.put("code", code.name());
        body.put("fieldErrors", fieldErrors == null ? List.of() : fieldErrors);
        return body;
    }

    public void write(HttpServletResponse response, ErrorCode code, String detail, String instance)
            throws IOException {
        HttpStatus status = code.status();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body(status, code, detail, instance, List.of()));
    }
}
