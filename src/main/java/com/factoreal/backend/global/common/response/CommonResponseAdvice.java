package com.factoreal.backend.global.common.response;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


@RestControllerAdvice(basePackages = "com.factoreal.backend")
public class CommonResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        String pkgName = returnType.getDeclaringClass().getPackageName();
        if (pkgName.startsWith("org.springdoc") || pkgName.contains("swagger") || pkgName.contains("springfox")) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response) {

        String uri = request.getURI().toString();
        if (uri.contains("/v3/api-docs") || uri.contains("/swagger") || uri.contains("/swagger-ui")) {
            return body;
        }

        if (body instanceof String || body instanceof Resource) {
            return body;
        }
        // ✅ 이미 래핑된 응답은 무시
        if (body instanceof CommonResponse<?>) {
            return body;
        }
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();
        HttpStatus httpStatus = HttpStatus.resolve(status);

        if (httpStatus != null && httpStatus.is2xxSuccessful()) {
            return CommonResponse.onSuccess(status, body);
        }

        return body;
    }
}
