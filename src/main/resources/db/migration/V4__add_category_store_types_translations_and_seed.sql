-- V4: Category ↔ StoreType M:N + per-locale translations.
--
-- Dev environment: containers are wiped (`docker compose down -v`) before
-- redeploy, so we use NOT NULL outright. Backfill rows for the 10 existing
-- canonical cuisine categories (idempotency for repeat runs is handled by
-- the seed runner, not by the migration).

-- 1. M:N: category ↔ store_type
CREATE TABLE category_store_types (
    category_id UUID         NOT NULL,
    store_type  VARCHAR(32)  NOT NULL,
    CONSTRAINT pk_category_store_types PRIMARY KEY (category_id, store_type),
    CONSTRAINT fk_cst_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE CASCADE
);
CREATE INDEX idx_cst_store_type ON category_store_types (store_type);

-- 2. Translations table
CREATE TABLE category_translations (
    category_id UUID         NOT NULL,
    locale      VARCHAR(16)  NOT NULL,        -- BCP-47: 'en', 'uk', 'en-US', 'zh-Hant-TW'
    name        VARCHAR(128) NOT NULL,
    CONSTRAINT pk_category_translations PRIMARY KEY (category_id, locale),
    CONSTRAINT fk_ct_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE CASCADE
);
CREATE INDEX idx_ct_locale ON category_translations (locale);

-- 3. Backfill: existing 10 cuisine categories all apply to RESTAURANT
INSERT INTO category_store_types (category_id, store_type)
SELECT id, 'RESTAURANT' FROM categories
WHERE slug IN ('pizza','sushi','asian','burgers','dessert','vegan','greek',
               'bakery','pastry','coffee');

-- Categories that *also* apply to non-restaurant types
INSERT INTO category_store_types (category_id, store_type)
SELECT id, 'BAKERY' FROM categories WHERE slug IN ('bakery','pastry');

INSERT INTO category_store_types (category_id, store_type)
SELECT id, 'CAFE'   FROM categories WHERE slug = 'coffee';

-- 4. Backfill: current categories.name becomes the canonical 'en' translation.
INSERT INTO category_translations (category_id, locale, name)
SELECT id, 'en', name FROM categories;
