package ru.mirea.parking.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.mirea.parking.security.AuthMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Преобразует любое исключение API в {@link ErrorResponse} с нужным HTTP-кодом. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String STALE_VERSION_MESSAGE = "Запись изменил другой пользователь, обновите карточку";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = e instanceof BadRequestException bad && bad.getField() != null
                ? List.of(new ErrorResponse.FieldError(bad.getField(), e.getReason()))
                : List.of();
        return build(e.getStatusCode(), e.getError(), e.getReason(), request, fields);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException e, HttpServletRequest request) {
        int status = e.getStatusCode().value();
        String message = e.getReason() == null ? ErrorCodes.defaultMessage(status) : e.getReason();
        return build(e.getStatusCode(), ErrorCodes.forStatus(status), message, request, List.of());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> validation(BindException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.isBindingFailure()
                        ? "Некорректное значение «" + error.getRejectedValue() + "»"
                        : error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, validationMessage(fields), request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = new ArrayList<>();
        e.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                fields.add(new ErrorResponse.FieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage()))));
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, validationMessage(fields), request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, validationMessage(fields), request, fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Некорректное значение параметра «" + e.getName() + "»";
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, message, request,
                List.of(new ErrorResponse.FieldError(e.getName(), message)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "Не указан обязательный параметр «" + e.getParameterName() + "»";
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, message, request,
                List.of(new ErrorResponse.FieldError(e.getParameterName(), message)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> missingPart(MissingServletRequestPartException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Не передан файл «" + e.getRequestPartName() + "»",
                request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (e.getCause() instanceof UnrecognizedPropertyException unknown) {
            String message = "Неизвестное поле «" + unknown.getPropertyName() + "»";
            return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, message, request,
                    List.of(new ErrorResponse.FieldError(unknown.getPropertyName(), message)));
        }
        if (e.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining(".")).replace(".[", "[");
            String message = "Некорректное значение поля «" + field + "»";
            return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, message, request,
                    List.of(new ErrorResponse.FieldError(field, message)));
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Некорректный JSON в теле запроса", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> unauthorized(AuthenticationException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, AuthMessages.LOGIN_REQUIRED, request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, AuthMessages.PERMISSION_DENIED, request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Адрес не найден", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.forStatus(405), ErrorCodes.defaultMessage(405), request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMedia(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.forStatus(415), ErrorCodes.defaultMessage(415), request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> notAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, ErrorCodes.forStatus(406), ErrorCodes.defaultMessage(406), request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCodes.forStatus(413), "Файл больше допустимого размера", request, List.of());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> staleVersion(Exception e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCodes.STALE_VERSION, STALE_VERSION_MESSAGE, request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, ErrorCodes.DATA_CONFLICT,
                "Запись конфликтует с уже существующими данными", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, ErrorCodes.defaultMessage(500), request, List.of());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatusCode status, String error, String message,
                                                       HttpServletRequest request, List<ErrorResponse.FieldError> fields) {
        ErrorResponse body = ErrorResponse.of(status.value(), error, message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static String validationMessage(List<ErrorResponse.FieldError> fields) {
        if (fields.isEmpty()) {
            return "Проверьте заполнение полей";
        }
        return "Проверьте поля: " + fields.stream().map(ErrorResponse.FieldError::field).distinct()
                .collect(Collectors.joining(", "));
    }

    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}
