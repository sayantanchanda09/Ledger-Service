package com.sayantan.ledger.ledger_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String idempotencyKey,
        String description,
        String status,
        Instant createdAt,
        List<EntryResponse> entries
) {}