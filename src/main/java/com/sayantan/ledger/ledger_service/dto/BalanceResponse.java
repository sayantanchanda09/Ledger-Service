package com.sayantan.ledger.ledger_service.dto;

import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        Long balanceMinorUnits,
        String currency
) {}
