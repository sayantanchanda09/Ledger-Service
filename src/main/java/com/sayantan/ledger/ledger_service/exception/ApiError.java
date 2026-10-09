package com.sayantan.ledger.ledger_service.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp,
        List<String> details
) {}
