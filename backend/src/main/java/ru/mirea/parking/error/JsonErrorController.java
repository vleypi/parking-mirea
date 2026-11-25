package ru.mirea.parking.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ответ /error в формате {@link ErrorResponse} для ошибок вне контроллеров. */
@RestController
public class JsonErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        int status = statusOf(request);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = uri == null ? request.getRequestURI() : uri.toString();
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(status, ErrorCodes.forStatus(status), ErrorCodes.defaultMessage(status), path));
    }

    private static int statusOf(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Integer code) {
            return code;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 500;
        }
    }
}
