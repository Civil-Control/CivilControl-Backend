CREATE TABLE custom_fuel_types (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT       NOT NULL,
    key       VARCHAR(50)  NOT NULL,
    label     VARCHAR(100) NOT NULL,
    deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_custom_fuel_types_tenant_key UNIQUE (tenant_id, key)
);
