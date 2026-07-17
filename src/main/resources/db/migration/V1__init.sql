-- food-service schema. Runs inside the `food` schema.
CREATE TABLE food_post (
    id            UUID PRIMARY KEY,
    giver_id      TEXT         NOT NULL,
    title         VARCHAR(120) NOT NULL,
    description   VARCHAR(4000),
    quantity      VARCHAR(80)  NOT NULL,
    food_type     TEXT         NOT NULL,
    dietary       TEXT         NOT NULL DEFAULT 'UNSPECIFIED',
    location_text VARCHAR(120),
    lat           DOUBLE PRECISION,
    lng           DOUBLE PRECISION,
    best_before   TIMESTAMPTZ,
    image_url     VARCHAR(1000),
    status        TEXT         NOT NULL DEFAULT 'AVAILABLE',
    claimed_by    TEXT,
    claimed_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_food_post_status ON food_post (status);
CREATE INDEX idx_food_post_type ON food_post (food_type);
CREATE INDEX idx_food_post_giver ON food_post (giver_id);
CREATE INDEX idx_food_post_claimed_by ON food_post (claimed_by);
CREATE INDEX idx_food_post_best_before ON food_post (best_before);
