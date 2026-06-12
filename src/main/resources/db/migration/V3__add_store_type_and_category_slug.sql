-- V3: introduce StoreType + Category.slug
--
-- Dev environment: containers are wiped (`docker compose down -v`) before
-- redeploy, so we add the columns as NOT NULL outright instead of the
-- 3-step nullable → backfill → NOT NULL dance.

ALTER TABLE categories ADD COLUMN slug      VARCHAR(64) NOT NULL;
ALTER TABLE categories ADD COLUMN icon_name VARCHAR(64);
ALTER TABLE categories ADD CONSTRAINT uq_categories_slug UNIQUE (slug);

ALTER TABLE stores ADD COLUMN type VARCHAR(32) NOT NULL;

CREATE INDEX idx_stores_type ON stores (type);
