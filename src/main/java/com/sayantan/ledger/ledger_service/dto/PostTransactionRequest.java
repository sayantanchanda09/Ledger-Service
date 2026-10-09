package com.sayantan.ledger.ledger_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostTransactionRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @Size(min = 2) List<@Valid EntryRequest> entries
) {}