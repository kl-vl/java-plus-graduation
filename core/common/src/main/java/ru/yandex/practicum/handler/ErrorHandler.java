package ru.yandex.practicum.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.dto.ErrorResponseDto;
import ru.yandex.practicum.exception.CategoryIsRelatedToEventException;
import ru.yandex.practicum.exception.CategoryNameUniqueException;
import ru.yandex.practicum.exception.CategoryNotFoundException;
import ru.yandex.practicum.exception.CommentNotFoundException;
import ru.yandex.practicum.exception.CompilationNotFoundException;
import ru.yandex.practicum.exception.EventAlreadyPublishedException;
import ru.yandex.practicum.exception.EventCanceledCantPublishException;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.EventNotPublishedException;
import ru.yandex.practicum.exception.EventValidationException;
import ru.yandex.practicum.exception.FilterValidationException;
import ru.yandex.practicum.exception.ParticipantLimitExceededException;
import ru.yandex.practicum.exception.RequestAlreadyExistsException;
import ru.yandex.practicum.exception.RequestSelfAttendException;
import ru.yandex.practicum.exception.UserAlreadyExistsException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private String extractParameterName(String propertyPath) {
        if (propertyPath.contains(".")) {
            String[] parts = propertyPath.split("\\.");
            return parts[parts.length - 1];
        }
        return propertyPath;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleInternalControllerValidationExceptions(final MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value"),
                        (existingMessage, newMessage) -> existingMessage + "; " + newMessage
                ));

        log.warn("Method argument validation error in {} : {}", request.getDescription(false), errors, ex);

        return new ErrorResponseDto("Validation failed", "VALIDATION_ERROR", errors);
    }

    @ExceptionHandler({EventDateException.class,
            MissingServletRequestParameterException.class,
            FilterValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto errorHandlerIncorrectDataExceptions(final Exception ex, final WebRequest request) {

        log.error("Input data is incorrect {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, String> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());

        return new ErrorResponseDto("Input data is incorrect", "BAD_REQUEST", details);
    }

    @ExceptionHandler({CategoryNotFoundException.class,
            EventNotFoundException.class,
            CompilationNotFoundException.class,
            CommentNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDto errorHandlerNotFound(final Exception ex, final WebRequest request) {

        log.error("Entity not found in {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, String> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());

        return new ErrorResponseDto("Entity not found", "NOT_FOUND", details);
    }

    @ExceptionHandler({CategoryNameUniqueException.class,
            EventValidationException.class,
            UserAlreadyExistsException.class,
            CategoryIsRelatedToEventException.class,
            EventAlreadyPublishedException.class,
            EventCanceledCantPublishException.class,
            RequestSelfAttendException.class,
            EventNotPublishedException.class,
            ParticipantLimitExceededException.class,
            RequestAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDto errorHandlerConflictExceptions(final Exception ex, final WebRequest request) {

        log.error("Data conflict in {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, String> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());

        return new ErrorResponseDto("Data conflict", "CONFLICT", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream().collect(Collectors.toMap(violation -> extractParameterName(violation.getPropertyPath().toString()), violation -> Optional.ofNullable(violation.getMessage()).orElse("Invalid value")));

        log.warn("Constraint violation error in {} : {}", request.getDescription(false), errors, ex);

        return new ErrorResponseDto("Validation failed", "VALIDATION_ERROR", errors);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto errorHandlerInternal(final Exception ex, final WebRequest request) {

        log.error("Internal error in {}: {}", request.getDescription(false), ex.getMessage(), ex);

        Map<String, String> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage());
        details.put("stackTrace", getStackTraceAsString(ex));

        return new ErrorResponseDto("Internal server error", "INTERNAL_ERROR", details);
    }

}