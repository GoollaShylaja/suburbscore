-- SuburbScore: suburb-service schema
-- Database: suburbscore_suburbs

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS cities (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    state      VARCHAR(10)  NOT NULL,
    country    VARCHAR(50)  NOT NULL DEFAULT 'Australia',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_city_name_state UNIQUE (name, state)
);

CREATE TABLE IF NOT EXISTS suburbs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id     UUID         REFERENCES cities(id),
    name        VARCHAR(100) NOT NULL,
    postcode    VARCHAR(4)   NOT NULL,
    lga         VARCHAR(100),
    latitude    NUMERIC(9,6) NOT NULL,
    longitude   NUMERIC(9,6) NOT NULL,
    region      VARCHAR(50),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suburb_postcode_name UNIQUE (postcode, name)
);

CREATE INDEX IF NOT EXISTS idx_suburbs_postcode   ON suburbs(postcode);
CREATE INDEX IF NOT EXISTS idx_suburbs_region     ON suburbs(region);
CREATE INDEX IF NOT EXISTS idx_suburbs_name       ON suburbs(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_suburbs_city_id    ON suburbs(city_id);
CREATE INDEX IF NOT EXISTS idx_suburbs_is_deleted ON suburbs(is_deleted);

CREATE TABLE IF NOT EXISTS suburb_stats (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id                 UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    median_rent_weekly        INTEGER,
    median_rent_updated_at    TIMESTAMP,
    crime_index               NUMERIC(5,2),
    crime_updated_at          TIMESTAMP,
    walkability_score         NUMERIC(4,1),
    walkability_amenity_count INTEGER,
    parks_count               INTEGER,
    pct_houses                INTEGER,
    pct_apartments            INTEGER,
    pct_townhouses            INTEGER,
    pct_units                 INTEGER,
    population                INTEGER,
    median_age                INTEGER,
    unemployment_rate         NUMERIC(4,2),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suburb_stats_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS suburb_rent_by_type (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id           UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    bedrooms            INTEGER      NOT NULL,
    property_type       VARCHAR(20)  NOT NULL,
    median_rent_weekly  INTEGER,
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rent_suburb_beds_type UNIQUE (suburb_id, bedrooms, property_type)
);

CREATE INDEX IF NOT EXISTS idx_rent_suburb_id ON suburb_rent_by_type(suburb_id);

CREATE TABLE IF NOT EXISTS transport_data (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id               UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    nearest_train_station   VARCHAR(100),
    train_station_walk_mins INTEGER,
    num_bus_routes          INTEGER,
    cbd_commute_mins_train  INTEGER,
    cbd_commute_mins_bus    INTEGER,
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transport_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS school_data (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    suburb_id           UUID         NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    num_primary_schools INTEGER      DEFAULT 0,
    num_high_schools    INTEGER      DEFAULT 0,
    avg_icsea_score     NUMERIC(6,2),
    best_school_name    VARCHAR(255),
    data_available      BOOLEAN      DEFAULT false,
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_school_suburb UNIQUE (suburb_id)
);

CREATE TABLE IF NOT EXISTS saved_suburbs (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID      NOT NULL,
    suburb_id   UUID      NOT NULL REFERENCES suburbs(id) ON DELETE CASCADE,
    saved_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saved_user_suburb UNIQUE (user_id, suburb_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_suburbs_user_id ON saved_suburbs(user_id);

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
