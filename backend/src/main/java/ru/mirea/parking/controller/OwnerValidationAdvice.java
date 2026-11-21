package ru.mirea.parking.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ошибки Bean Validation для API владельцев: 400 с перечнем полей
 * {@code errors: {"phone": ["Телефон в формате +7-XXX-XXX-XX-XX"]}}, этот формат понимает web-клиент.
 * Действует только на контроллеры владельцев; когда появится общий обработчик ошибок, этот класс можно убрать.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OwnerController.class)
public class OwnerValidationAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handle(MethodArgumentNotValidException e) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), f -> new ArrayList<>()).add(error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте поля: " + String.join(", ", errors.keySet()));
        problem.setTitle("Ошибка в данных");
        problem.setProperty("errors", errors);
        return problem;
    }
}
