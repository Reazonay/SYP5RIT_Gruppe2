package at.restaurant;

import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
class ApiErrors {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> domain(ApiException e) {
    return ResponseEntity.status(e.status()).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  ResponseEntity<?> validation(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("message", "Eingaben ungültig"));
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<?> database(DataAccessException e) {
    Throwable cause = e.getMostSpecificCause();
    String state = cause instanceof java.sql.SQLException sql ? sql.getSQLState() : "";
    if (java.util.Set.of("23505", "23P01", "40P01", "40001").contains(state))
      return ResponseEntity.status(409)
          .body(
              Map.of(
                  "message",
                  "Konflikt mit einer bestehenden Buchung oder Transaktion. Bitte erneut prüfen."));
    return internal(e);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<?> forbidden(Exception e) {
    return ResponseEntity.status(403).body(Map.of("message", "Keine Berechtigung"));
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  ResponseEntity<?> missingResource(Exception e) {
    return ResponseEntity.status(404).body(Map.of("message", "Nicht gefunden"));
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<?> wrongMethod(Exception e) {
    return ResponseEntity.status(405).body(Map.of("message", "Methode nicht erlaubt"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> internal(Exception e) {
    LoggerFactory.getLogger(ApiErrors.class)
        .error("Request failed: {}", e.getClass().getSimpleName());
    return ResponseEntity.internalServerError()
        .body(Map.of("message", "Interner Fehler. Bitte erneut versuchen."));
  }
}
