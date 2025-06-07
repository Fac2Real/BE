package com.factoreal.backend.global.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;

    // 정적 팩토리 메서드(선택)
    public static ErrorResponse of(String code, String message){
        return new ErrorResponse(code, message);
    }
}