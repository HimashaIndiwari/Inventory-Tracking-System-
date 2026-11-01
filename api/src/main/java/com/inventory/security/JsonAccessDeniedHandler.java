package com.inventory.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.inventory.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403 problem body when the role does not allow the request. */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter writer;

    public JsonAccessDeniedHandler(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        writer.write(response, ErrorCode.FORBIDDEN, "You do not have permission to perform this action.",
                request.getRequestURI());
    }
}
