package edu.scau.scauarchiveinsight.config;

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
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // 统一把 @Valid 的参数校验异常转成前端约定的数据结构
        String message = "请求参数错误";
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("message", message);
        body.put("success", false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
