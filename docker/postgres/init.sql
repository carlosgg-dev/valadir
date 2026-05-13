CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE account_status AS ENUM ('PENDING_ACTIVATION', 'ACTIVE');

-- Accounts: Authentication Data
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'USER',
    status account_status NOT NULL DEFAULT 'PENDING_ACTIVATION',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Users: Profile Data
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    account_id UUID UNIQUE NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    given_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Partial index for the expired accounts pending activation purge job.
CREATE INDEX index_accounts_pending_created_at ON accounts(created_at) WHERE status = 'PENDING_ACTIVATION';
