package com.sayantan.ledger.ledger_service.service;

import com.sayantan.ledger.ledger_service.domain.Account;
import com.sayantan.ledger.ledger_service.domain.enums.AccountType;
import com.sayantan.ledger.ledger_service.dto.AccountResponse;
import com.sayantan.ledger.ledger_service.dto.BalanceResponse;
import com.sayantan.ledger.ledger_service.dto.CreateAccountRequest;
import com.sayantan.ledger.ledger_service.repository.AccountRepository;
import com.sayantan.ledger.ledger_service.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository,
                          LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .ownerName(request.ownerName())
                .type(AccountType.valueOf(request.accountType().toUpperCase()))
                .currency(request.currency().toUpperCase())
                .build();

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        return toResponse(account);
    }

    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));

        Long balance = ledgerEntryRepository.computeBalance(accountId);
        return new BalanceResponse(accountId, balance, account.getCurrency());
    }

    private String generateAccountNumber() {
        // Simple unique-ish generator for now; we'll revisit this later if needed
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getType().name(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}