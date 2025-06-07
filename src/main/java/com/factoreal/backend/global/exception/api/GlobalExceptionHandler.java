package com.factoreal.backend.global.exception.api;

import com.factoreal.backend.global.common.response.CommonResponse;
import com.factoreal.backend.global.exception.dto.BadRequestException;
import com.factoreal.backend.global.exception.dto.DuplicateResourceException;
import com.factoreal.backend.global.exception.dto.ErrorResponse;
import com.factoreal.backend.global.exception.dto.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResponse<?> handleNotFoundException(NotFoundException e) {
        return CommonResponse.onFailure(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResponse<?> handleBadRequestException(BadRequestException e) {
        return CommonResponse.onFailure(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateResourceException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE", ex.getMessage()));
    }
}
