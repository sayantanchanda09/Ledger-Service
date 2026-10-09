package com.sayantan.ledger.ledger_service.exception;

import java.util.NoSuchElementException;
import java.util.UUID;

public class AccountNotFoundException extends NoSuchElementException {
    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id);
    }
}