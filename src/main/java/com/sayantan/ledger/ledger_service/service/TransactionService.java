package com.sayantan.ledger.ledger_service.service;

import com.sayantan.ledger.ledger_service.domain.Account;
import com.sayantan.ledger.ledger_service.domain.LedgerEntry;
import com.sayantan.ledger.ledger_service.domain.Transaction;
import com.sayantan.ledger.ledger_service.domain.enums.AccountType;
import com.sayantan.ledger.ledger_service.domain.enums.EntryType;
import com.sayantan.ledger.ledger_service.domain.enums.TransactionStatus;
import com.sayantan.ledger.ledger_service.dto.EntryRequest;
import com.sayantan.ledger.ledger_service.dto.EntryResponse;
import com.sayantan.ledger.ledger_service.dto.PostTransactionRequest;
import com.sayantan.ledger.ledger_service.dto.TransactionResponse;
import com.sayantan.ledger.ledger_service.exception.AccountNotFoundException;
import com.sayantan.ledger.ledger_service.exception.InsufficientFundsException;
import com.sayantan.ledger.ledger_service.exception.InvalidTransactionException;
import com.sayantan.ledger.ledger_service.repository.AccountRepository;
import com.sayantan.ledger.ledger_service.repository.LedgerEntryRepository;
import com.sayantan.ledger.ledger_service.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public TransactionResponse post(String idempotencyKey, PostTransactionRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new InvalidTransactionException("Idempotency-Key must be between 1 and 255 characters");
        }

        // Simple replay check. A race between two identical simultaneous requests
        // is handled properly in the idempotency step.
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        List<EntryRequest> entries = request.entries();

        // 1. Load every referenced account; fail if any is missing
        Set<UUID> ids = entries.stream().map(EntryRequest::accountId).collect(Collectors.toSet());
        Map<UUID, Account> accounts = accountRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
        for (UUID id : ids) {
            if (!accounts.containsKey(id)) {
                throw new AccountNotFoundException(id);
            }
        }

        // 2. v1 rule: one currency per transaction
        Set<String> currencies = accounts.values().stream()
                .map(Account::getCurrency).collect(Collectors.toSet());
        if (currencies.size() != 1) {
            throw new InvalidTransactionException("All accounts in a transaction must share one currency");
        }

        // 3. Debits must equal credits; track the net effect per account
        long totalDebits = 0;
        long totalCredits = 0;
        Map<UUID, Long> netByAccount = new HashMap<>();
        for (EntryRequest e : entries) {
            EntryType type = EntryType.valueOf(e.entryType().toUpperCase());
            long amount = e.amountMinorUnits();
            if (type == EntryType.DEBIT) {
                totalDebits = Math.addExact(totalDebits, amount);
                netByAccount.merge(e.accountId(), -amount, Math::addExact);
            } else {
                totalCredits = Math.addExact(totalCredits, amount);
                netByAccount.merge(e.accountId(), amount, Math::addExact);
            }
        }
        if (totalDebits != totalCredits) {
            throw new InvalidTransactionException(
                    "Unbalanced transaction: debits=" + totalDebits + ", credits=" + totalCredits);
        }

        // 4. Customer accounts may not go below zero (SYSTEM accounts may)
        for (Map.Entry<UUID, Long> net : netByAccount.entrySet()) {
            Account account = accounts.get(net.getKey());
            if (account.getType() == AccountType.USER && net.getValue() < 0) {
                long current = ledgerEntryRepository.computeBalance(account.getId());
                if (current + net.getValue() < 0) {
                    throw new InsufficientFundsException(
                            "Insufficient funds in account " + account.getAccountNumber());
                }
            }
        }

        // 5. Build and save the transaction with all its entries in one DB transaction
        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .description(request.description())
                .status(TransactionStatus.POSTED)
                .build();

        for (EntryRequest e : entries) {
            Account account = accounts.get(e.accountId());
            transaction.addEntry(LedgerEntry.builder()
                    .account(account)
                    .entryType(EntryType.valueOf(e.entryType().toUpperCase()))
                    .amountMinorUnits(e.amountMinorUnits())
                    .currency(account.getCurrency())
                    .build());
        }

        return toResponse(transactionRepository.save(transaction));
    }

    private TransactionResponse toResponse(Transaction t) {
        List<EntryResponse> entryResponses = t.getEntries().stream()
                .map(e -> new EntryResponse(
                        e.getAccount().getId(),
                        e.getEntryType().name(),
                        e.getAmountMinorUnits(),
                        e.getCurrency()))
                .toList();
        return new TransactionResponse(
                t.getId(), t.getIdempotencyKey(), t.getDescription(),
                t.getStatus().name(), t.getCreatedAt(), entryResponses);
    }
}