CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS markets (
                                       market_id TEXT PRIMARY KEY, -- for ex. "EUR-USD"
                                       base_token TEXT NOT NULL,
                                       quote_token TEXT NOT NULL,
                                       price NUMERIC(38, 18) NOT NULL CHECK (price > 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
    );
CREATE INDEX IF NOT EXISTS idx_markets_base ON markets(base_token);
CREATE INDEX IF NOT EXISTS idx_markets_quote ON markets(quote_token);


CREATE TABLE IF NOT EXISTS conversions (
                                           conversion_id UUID PRIMARY KEY,
                                           command_id UUID NOT NULL UNIQUE,
                                           idempotency_key TEXT NULL,
                                           token_from TEXT NOT NULL,
                                           token_to TEXT NOT NULL,
                                           amount_in NUMERIC(38, 18) NOT NULL CHECK (amount_in > 0),
    amount_out NUMERIC(38, 18) NOT NULL DEFAULT 0,
    path_json JSONB NOT NULL DEFAULT '[]',
    computed_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('SUCCEEDED','FAILED','NO_ROUTE'))
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_conversions_idem_key ON conversions(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_conversions_cmd ON conversions(command_id);


CREATE TABLE IF NOT EXISTS outbox (
                                      event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      aggregate_type TEXT NOT NULL,
                                      aggregate_id TEXT NOT NULL,
                                      event_type TEXT NOT NULL,
                                      payload_json JSONB NOT NULL,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ NULL
    );
CREATE INDEX IF NOT EXISTS idx_outbox_published_created ON outbox(published, created_at);

CREATE TABLE IF NOT EXISTS shedlock (
    name        VARCHAR(64)  PRIMARY KEY,
    lock_until  TIMESTAMPTZ  NOT NULL,
    locked_at   TIMESTAMPTZ  NOT NULL,
    locked_by   VARCHAR(255) NOT NULL
);
