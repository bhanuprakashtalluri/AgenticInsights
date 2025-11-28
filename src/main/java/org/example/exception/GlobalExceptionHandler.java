package org.example.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String errorCode, HttpServletRequest request, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", errorCode);
        body.put("message", message);
        if (request != null) body.put("path", request.getRequestURI());
        if (extra != null) body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String name = ex.getName();
        Object valueObj = ex.getValue();
        String value = valueObj == null ? "null" : valueObj.toString();
        String requiredType = ex.getRequiredType() == null ? "unknown" : ex.getRequiredType().getSimpleName();
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s.", value, name, requiredType);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("parameter", name);
        extra.put("expectedType", requiredType);
        extra.put("provided", value);
        return build(HttpStatus.BAD_REQUEST, message, "INVALID_PARAMETER", req, extra);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("part", ex.getRequestPartName());
        extra.put("hint", "Ensure form-data field name is 'file' and Content-Type is multipart/form-data");
        String message = "Required multipart part is missing: '" + ex.getRequestPartName() + "'";
        return build(HttpStatus.BAD_REQUEST, message, "MISSING_MULTIPART_PART", req, extra);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("hint", "Reduce file size or increase spring.servlet.multipart.max-file-size and max-request-size");
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds maximum allowed size", "FILE_TOO_LARGE", req, extra);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest req) {
        // Avoid leaking internals; provide simple message. Log stack trace separately via logger.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", "INTERNAL_ERROR", req, Map.of("detail", ex.getClass().getSimpleName()));
    }
}
