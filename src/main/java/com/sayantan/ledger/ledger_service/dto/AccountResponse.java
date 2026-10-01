package com.sayantan.ledger.ledger_service.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        String type,
        String currency,
        Instant createdAt
) {}
