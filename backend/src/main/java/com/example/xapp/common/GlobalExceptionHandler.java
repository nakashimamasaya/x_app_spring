package com.example.xapp.common;

import com.example.xapp.common.exception.AppException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全エラーを RFC 9457 Problem Details に統一する。
 *
 * <p>{@code type} が機械可読な識別子で、クライアントはこれで分岐する。
 * {@code title} と {@code detail} の文面は変わりうる。
 *
 * <p><strong>スタックトレースや内部の識別子をレスポンスに含めない。</strong>
 * public な API なので、内部構造の手がかりを与えない。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** バリデーション違反時に、どのフィールドが不正かを返す。 */
    public record FieldError(String field, String message) {}

    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException e) {
        return problem(e.getStatus(), e.getType(), e.getTitle(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "urn:x-app-spring:problem:validation-failed",
                        "Validation failed",
                        "入力値に誤りがあります。");

        List<FieldError> errors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> new FieldError(f.getField(), f.getDefaultMessage()))
                        .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    /** 壊れた JSON など、ボディが読めない場合。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:x-app-spring:problem:validation-failed",
                "Validation failed",
                "リクエストボディを解釈できません。");
    }

    /**
     * パス変数の型変換失敗。{@code /posts/abc} のように UUID 形式ですらない値が来た場合で、
     * 存在しない UUID の 404 とは区別する（api/openapi.yaml の InvalidPostId）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:x-app-spring:problem:invalid-path-parameter",
                "Invalid path parameter",
                "%s は %s 形式で指定してください。"
                        .formatted(e.getName(), e.getRequiredType() == null
                                ? "正しい" : e.getRequiredType().getSimpleName()));
    }

    private static ProblemDetail problem(
            HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
