-- Content tables for hero images and category icons (from monolith content schema)

CREATE TABLE hero_images (
    id   SERIAL PRIMARY KEY,
    key  VARCHAR(50) UNIQUE NOT NULL,
    url  VARCHAR(500)       NOT NULL,
    icon VARCHAR(50)
);

CREATE TABLE category_icons (
    id        SERIAL PRIMARY KEY,
    key       VARCHAR(50) UNIQUE NOT NULL,
    icon_name VARCHAR(50)        NOT NULL
);

-- Insert initial category icons
INSERT INTO category_icons (key, icon_name) VALUES
    ('bakery', 'croissant'),
    ('cafe', 'coffee'),
    ('restaurant', 'utensils'),
    ('grocery', 'shopping-basket'),
    ('fast_food', 'hamburger');
