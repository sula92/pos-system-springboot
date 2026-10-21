package com.idet.pos.config;

import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps error responses from other POS services back to the shared business exceptions,
 * so a 404 or 409 from customer-service or item-service keeps its meaning in order-service.
 */
@Configuration
public class FeignClientConfig {

    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Bean
    public ErrorDecoder posErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            String message = extractMessage(response);
            return switch (response.status()) {
                case 400 -> new InvalidRequestException(message != null ? message : "Invalid request");
                case 404 -> new ResourceNotFoundException(message != null ? message : "Resource not found");
                case 409 -> new InsufficientStockException(message != null ? message : "Insufficient stock");
                default -> defaultDecoder.decode(methodKey, response);
            };
        };
    }

    private static String extractMessage(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream body = response.body().asInputStream()) {
            String text = new String(Util.toByteArray(body), StandardCharsets.UTF_8);
            Matcher matcher = MESSAGE_FIELD.matcher(text);
            return matcher.find() ? matcher.group(1) : null;
        } catch (IOException ex) {
            return null;
        }
    }
}
