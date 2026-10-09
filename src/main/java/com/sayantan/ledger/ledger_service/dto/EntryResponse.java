package com.sayantan.ledger.ledger_service.dto;

import java.util.UUID;

public record EntryResponse(
        UUID accountId,
        String entryType,
        Long amountMinorUnits,
        String currency
) {}