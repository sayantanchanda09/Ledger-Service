-- Accounts: no balance column by design; balance is always derived from ledger_entries
CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    account_number  VARCHAR(64) NOT NULL UNIQUE,
    owner_name      VARCHAR(255) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

--Transactions: one logical financial event, atomic, idempotent
CREATE TABLE transactions (
    id               UUID PRIMARY KEY,
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    description       VARCHAR(500) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);

-- Ledger entries: immutable debit/credit rows, always in pairs (or more) per transaction
CREATE TABLE ledger_entries (
                                id                  UUID PRIMARY KEY,
                                transaction_id      UUID NOT NULL REFERENCES transactions(id),
                                account_id          UUID NOT NULL REFERENCES accounts(id),
                                entry_type          VARCHAR(10) NOT NULL,
                                amount_minor_units  BIGINT NOT NULL CHECK (amount_minor_units > 0),
                                currency            VARCHAR(3) NOT NULL,
                                created_at          TIMESTAMPTZ NOT NULL
);

-- Indexes for the queries we'll run constantly: balance lookups and transaction history
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);