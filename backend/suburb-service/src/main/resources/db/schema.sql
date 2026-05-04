-- SuburbScore Database Schema
-- Run this before starting the service for the first time.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS suburbs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    postcode    VARCHAR(4)   NOT NULL,
    lga         VARCHAR(100),
    latitude    NUMERIC(9,6) NOT NULL,
    longitude   NUMERIC(9,6) NOT NULL,
    region      VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suburb_postcode_name UNIQUE (postcode, name)
);

CREATE INDEX IF NOT EXISTS idx_suburbs_postcode ON suburbs(postcode);
CREATE INDEX IF NOT EXISTS idx_suburbs_region   ON suburbs(region);

CREATE TABLE IF NOT EXISTS suburb_stats (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id              UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    median_rent_weekly     INTEGER,
    median_rent_updated_at TIMESTAMP,
    crime_index            NUMERIC(5,2),
    crime_updated_at       TIMESTAMP,
    walkability_score      NUMERIC(4,1),
    population             INTEGER,
    median_age             INTEGER,
    unemployment_rate      NUMERIC(4,2),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suburb_stats_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS transport_data (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id              UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    nearest_train_station  VARCHAR(100),
    train_station_walk_mins INTEGER,
    num_bus_routes         INTEGER,
    cbd_commute_mins_train INTEGER,
    cbd_commute_mins_bus   INTEGER,
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transport_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS school_data (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id           UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    num_primary_schools INTEGER,
    num_high_schools    INTEGER,
    avg_naplan_score    NUMERIC(5,1),
    best_school_name    VARCHAR(150),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_school_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS suburb_scores (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id         UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    user_id           UUID         NOT NULL,
    total_score       NUMERIC(5,2),
    commute_score     NUMERIC(5,2),
    safety_score      NUMERIC(5,2),
    schools_score     NUMERIC(5,2),
    walkability_score NUMERIC(5,2),
    value_score       NUMERIC(5,2),
    calculated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suburb_score_user UNIQUE (suburb_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_suburb_scores_user_id   ON suburb_scores(user_id);
CREATE INDEX IF NOT EXISTS idx_suburb_scores_suburb_id ON suburb_scores(suburb_id);
