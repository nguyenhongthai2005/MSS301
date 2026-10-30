package fu.de190299.reservation.common;

import fu.de190299.reservation.dto.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiResponseDTO response = ApiResponseDTO.of(2, "Data validation failed", errors);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGeneralException(Exception ex) {
        ApiResponseDTO response = ApiResponseDTO.of(0, "Internal server error: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
