package com.razer.engine.exception;

import java.time.Instant;

public record ApiErrorResponse(
        String error,
        String message,
        Instant timestamp,
        String path
) {
}