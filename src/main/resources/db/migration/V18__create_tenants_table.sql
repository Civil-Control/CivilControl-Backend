CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    cuit VARCHAR(13) NOT NULL UNIQUE,
    legal_name VARCHAR(200),
    street VARCHAR(200),
    number INT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    zip_code VARCHAR(20),
    phone VARCHAR(30),
    email VARCHAR(100),
    logo_url VARCHAR(500),
    founded_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Seed default tenant (ESEA SA) to ensure company settings work from first boot
INSERT INTO tenants (id, name, cuit, legal_name, city, state, country, active, deleted)
VALUES (1, 'ESEA SA', '30-12345678-9', 'ESEA Sociedad Anónima', 'Buenos Aires', 'Buenos Aires', 'Argentina', TRUE, FALSE);
