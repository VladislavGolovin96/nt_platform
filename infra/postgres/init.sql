-- Database is created by POSTGRES_DB env var in Docker
-- This script runs additional initialization if needed

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE loadtest TO loadtest;
