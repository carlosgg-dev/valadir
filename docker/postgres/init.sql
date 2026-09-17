-- Accounts: Authentication Data
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    language VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users: Profile Data
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    account_id UUID UNIQUE NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL,
    given_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Partial index for the expired accounts pending activation purge job.
CREATE INDEX index_accounts_pending_created_at ON accounts(created_at) WHERE status = 'PENDING_ACTIVATION';
