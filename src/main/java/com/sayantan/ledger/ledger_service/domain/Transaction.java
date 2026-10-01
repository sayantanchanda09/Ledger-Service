package com.sayantan.ledger.ledger_service.domain;

import com.sayantan.ledger.ledger_service.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> entries = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null){
            this.status = TransactionStatus.PENDING;
        }
    }

    public void addEntry(LedgerEntry entry){
        entries.add(entry);
        entry.setTransaction(this);
    }
}
