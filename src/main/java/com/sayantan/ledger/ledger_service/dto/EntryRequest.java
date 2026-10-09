package com.sayantan.ledger.ledger_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record EntryRequest(
        @NotNull UUID accountId,
        @NotBlank String entryType, // "DEBIT" or "CREDIT"
        @NotNull @Positive Long amountMinorUnits
) {}