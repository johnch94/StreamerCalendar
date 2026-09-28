package com.example.streamercalendar.exception;

import com.example.streamercalendar.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * 모든 에러를 공통 포맷({ code, message, timestamp })으로 응답한다.
 *
 * Spring MVC 표준 예외(잘못된 파라미터 타입, 깨진 JSON, 없는 경로, 지원하지 않는 메서드 등)는
 * ResponseEntityExceptionHandler가 알맞은 상태 코드(400/404/405/415 등)와 헤더(예: 405의 Allow)를 정해주고,
 * 여기서는 handleExceptionInternal에서 응답 본문만 공통 포맷으로 바꾼다.
 * 필터 단계의 401/403(Spring Security)은 SecurityConfig에서 같은 포맷으로 응답한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_REQUEST", ex.getMessage()));
    }

    // 로그인 실패. 아이디/비밀번호 중 무엇이 틀렸는지는 알려주지 않는다
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("처리하지 못한 예외", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("MVC 처리 중 서버 오류", ex);
        }
        ErrorResponse error = ErrorResponse.of(codeOf(statusCode), messageOf(ex));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    // 400은 기존 스펙대로 INVALID_REQUEST, 나머지는 상태 이름(NOT_FOUND, METHOD_NOT_ALLOWED 등)을 코드로 쓴다
    private String codeOf(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
            return "INVALID_REQUEST";
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.name() : "HTTP_" + statusCode.value();
    }

    private String messageOf(Exception ex) {
        return switch (ex) {
            case MethodArgumentNotValidException e -> e.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .orElse("요청 값이 올바르지 않습니다.");
            case HandlerMethodValidationException e -> e.getParameterValidationResults().stream()
                    .findFirst()
                    .map(GlobalExceptionHandler::describe)
                    .orElse("요청 값이 올바르지 않습니다.");
            case MethodArgumentTypeMismatchException e ->
                    "'%s' 값이 올바르지 않습니다: %s".formatted(e.getName(), e.getValue());
            case TypeMismatchException e ->
                    "'%s' 값이 올바르지 않습니다: %s".formatted(e.getPropertyName(), e.getValue());
            case MissingServletRequestParameterException e ->
                    "필수 파라미터가 없습니다: " + e.getParameterName();
            case HttpMessageNotReadableException e -> describe(e);
            case HttpRequestMethodNotSupportedException e ->
                    "지원하지 않는 요청 메서드입니다: " + e.getMethod();
            case HttpMediaTypeNotSupportedException e ->
                    "지원하지 않는 Content-Type입니다. application/json으로 보내주세요.";
            case NoResourceFoundException e -> "요청한 경로를 찾을 수 없습니다: /" + e.getResourcePath();
            default -> "요청을 처리할 수 없습니다.";
        };
    }

    private static String describe(ParameterValidationResult result) {
        String message = result.getResolvableErrors().isEmpty()
                ? "값이 올바르지 않습니다."
                : result.getResolvableErrors().get(0).getDefaultMessage();
        return result.getMethodParameter().getParameterName() + ": " + message;
    }

    // 깨진 JSON이면 일반 문구, 특정 필드의 형식(enum, 날짜 등)이 틀렸으면 필드 이름을 알려준다
    private static String describe(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().getLast().getPropertyName();
            if (field != null) {
                return "'%s' 값의 형식이 올바르지 않습니다.".formatted(field);
            }
        }
        return "요청 본문(JSON) 형식이 올바르지 않습니다.";
    }
}
