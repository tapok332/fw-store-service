-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Categories
CREATE TABLE categories (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Stores
CREATE TABLE stores (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(512),
    hero_image_url  VARCHAR(512),
    category_id     UUID REFERENCES categories(id),
    address         VARCHAR(512),
    location        GEOGRAPHY(Point, 4326),
    rating          NUMERIC(3, 1),
    opens_at        TIME,
    closes_at       TIME,
    phone           VARCHAR(50),
    website         VARCHAR(512),
    delivery_fee_amount_minor   BIGINT,
    delivery_fee_currency       VARCHAR(3),
    min_order_amount_minor      BIGINT,
    min_order_currency          VARCHAR(3),
    price_level     INT,
    store_payload   JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stores_location ON stores USING GIST (location);
CREATE INDEX idx_stores_category_id ON stores (category_id);

CREATE TRIGGER trg_stores_updated_at
    BEFORE UPDATE ON stores
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Menu sections
CREATE TABLE menu_sections (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id   UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_sections_store_id ON menu_sections (store_id);

CREATE TRIGGER trg_menu_sections_updated_at
    BEFORE UPDATE ON menu_sections
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Store menu items
CREATE TABLE store_menu_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price_amount_minor  BIGINT NOT NULL,
    price_currency      VARCHAR(3) NOT NULL,
    image_url           VARCHAR(512),
    legacy_category     VARCHAR(255),
    available       BOOLEAN NOT NULL DEFAULT TRUE,
    store_id        UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    section_id      UUID REFERENCES menu_sections(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_menu_items_store_id ON store_menu_items (store_id);
CREATE INDEX idx_store_menu_items_section_id ON store_menu_items (section_id);

CREATE TRIGGER trg_store_menu_items_updated_at
    BEFORE UPDATE ON store_menu_items
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Store hours
CREATE TABLE store_hours (
    store_id    UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL,
    opens       TIME,
    closes      TIME,
    is_closed   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (store_id, day_of_week)
);

-- Combos
CREATE TABLE combos (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id   UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    price_amount_minor  BIGINT NOT NULL,
    price_currency      VARCHAR(3) NOT NULL,
    image_url           VARCHAR(512),
    savings             NUMERIC(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_combos_store_id ON combos (store_id);

CREATE TRIGGER trg_combos_updated_at
    BEFORE UPDATE ON combos
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Combo items (many-to-many)
CREATE TABLE combo_items (
    combo_id     UUID NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES store_menu_items(id) ON DELETE CASCADE,
    PRIMARY KEY (combo_id, menu_item_id)
);

-- Surprise boxes
CREATE TABLE surprise_boxes (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id           UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    price_amount_minor         BIGINT NOT NULL,
    price_currency             VARCHAR(3) NOT NULL,
    retail_price_amount_minor  BIGINT,
    retail_price_currency      VARCHAR(3),
    image_url                  VARCHAR(512),
    pickup_from        TIME,
    pickup_to          TIME,
    stock              INT NOT NULL DEFAULT 0,
    delivery_available BOOLEAN NOT NULL DEFAULT FALSE,
    rating             NUMERIC(3, 1),
    recommended        BOOLEAN NOT NULL DEFAULT FALSE,
    category           VARCHAR(255),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_surprise_boxes_store_id ON surprise_boxes (store_id);

CREATE TRIGGER trg_surprise_boxes_updated_at
    BEFORE UPDATE ON surprise_boxes
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Store reviews
CREATE TABLE store_reviews (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id   UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL,
    order_id   UUID,
    rating     SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_reviews_store_id ON store_reviews (store_id);

CREATE TRIGGER trg_store_reviews_updated_at
    BEFORE UPDATE ON store_reviews
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Store promos
CREATE TABLE store_promos (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id     UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    emoji        VARCHAR(10),
    bg_color     VARCHAR(20),
    accent_color VARCHAR(20),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    priority     INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_promos_store_id ON store_promos (store_id);

CREATE TRIGGER trg_store_promos_updated_at
    BEFORE UPDATE ON store_promos
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Outbox events (for transactional outbox pattern)
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type     VARCHAR(255) NOT NULL,
    topic          VARCHAR(255) NOT NULL,
    event_key      VARCHAR(255),
    payload        TEXT NOT NULL,
    correlation_id UUID,
    published      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMP
);

-- Processed events (for idempotent consumers)
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
