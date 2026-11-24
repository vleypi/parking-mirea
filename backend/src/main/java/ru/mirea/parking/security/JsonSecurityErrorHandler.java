package ru.mirea.parking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import ru.mirea.parking.error.ErrorCodes;
import ru.mirea.parking.error.ErrorResponse;

import java.io.IOException;

/** Ответы Spring Security 401/403 в формате {@link ErrorResponse}. */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        if (isSwagger(request.getRequestURI())) {
            response.sendRedirect("/swagger-login.html");
            return;
        }
        write(request, response, HttpStatus.UNAUTHORIZED, AuthMessages.LOGIN_REQUIRED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, AuthMessages.PERMISSION_DENIED);
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        ErrorResponse body = ErrorResponse.of(status.value(), ErrorCodes.forStatus(status.value()), message,
                request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private boolean isSwagger(String uri) {
        return uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }
}
