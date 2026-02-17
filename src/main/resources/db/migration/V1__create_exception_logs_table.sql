-- ============================================
-- Flyway Migration V1
-- Create Exception Log Table
-- ============================================
-- Author: Maximo Andriola
-- Date: 2026-02-17
-- Description: Creates the exception_logs table for async exception tracking
-- ============================================

CREATE TABLE IF NOT EXISTS exception_logs (
    id BIGSERIAL PRIMARY KEY,
    exception_name VARCHAR(255) NOT NULL,
    message TEXT,
    path VARCHAR(500),
    method VARCHAR(10),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stack_trace TEXT,

    -- Indexes for better query performance
    CONSTRAINT exception_logs_pkey PRIMARY KEY (id)
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_exception_logs_timestamp ON exception_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_exception_logs_exception_name ON exception_logs(exception_name);
CREATE INDEX IF NOT EXISTS idx_exception_logs_path ON exception_logs(path);

-- Add comments for documentation
COMMENT ON TABLE exception_logs IS 'Stores all exceptions caught by GlobalExceptionHandler for monitoring and debugging';
COMMENT ON COLUMN exception_logs.id IS 'Primary key - auto-generated';
COMMENT ON COLUMN exception_logs.exception_name IS 'Fully qualified exception class name';
COMMENT ON COLUMN exception_logs.message IS 'Exception message';
COMMENT ON COLUMN exception_logs.path IS 'Request path where exception occurred';
COMMENT ON COLUMN exception_logs.method IS 'HTTP method (GET, POST, etc.)';
COMMENT ON COLUMN exception_logs.timestamp IS 'When the exception occurred';
COMMENT ON COLUMN exception_logs.stack_trace IS 'Full stack trace for debugging';

