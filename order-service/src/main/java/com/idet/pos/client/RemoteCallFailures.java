package com.idet.pos.client;

import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.exception.ServiceUnavailableException;

/**
 * Decides what a Feign fallback throws.
 * Business errors from the remote service (404, 409, 400) are re-thrown unchanged so callers
 * keep the same HTTP status. Anything else (connection refused, timeout, open circuit, 5xx)
 * becomes a 503.
 */
final class RemoteCallFailures {

    private RemoteCallFailures() {
    }

    static RuntimeException toException(String serviceName, Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof ResourceNotFoundException
                    || current instanceof InsufficientStockException
                    || current instanceof InvalidRequestException) {
                return (RuntimeException) current;
            }
            current = current.getCause();
        }
        return new ServiceUnavailableException(serviceName + " is currently unavailable. Please try again later.", cause);
    }
}
