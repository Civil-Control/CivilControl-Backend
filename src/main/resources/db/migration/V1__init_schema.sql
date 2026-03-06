-- =============================================================================
-- V1__init_schema.sql
-- CivilControl Backend - Migración inicial del esquema completo
-- Generado a partir de las entidades JPA (Spring Boot 3.3.0 / Hibernate 6)
-- Orden: respeta dependencias de FK (tablas padre antes que tablas hijo)
-- IDEMPOTENTE: usa IF NOT EXISTS en todas las sentencias para soportar
-- bases de datos pre-existentes sin fallar.
-- =============================================================================

-- =============================================================================
-- SEGURIDAD: permissions, credentials, roles, users
-- (Permission NO extiende TenantEntity → sin tenant_id)
-- =============================================================================

CREATE TABLE IF NOT EXISTS permissions (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL UNIQUE,
    module               VARCHAR(50)  NOT NULL,
    work_module          VARCHAR(50),
    description          VARCHAR(255),
    spanish_translation  VARCHAR(150),
    spanish_description  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS credentials (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_credentials_tenant_username UNIQUE (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL,
    deleted     BOOLEAN      NOT NULL,
    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS users (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    credentials_id BIGINT,
    email          VARCHAR(100) NOT NULL,
    first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    job_title      VARCHAR(100),
    enabled        BOOLEAN      NOT NULL,
    deleted        BOOLEAN      NOT NULL,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_credentials FOREIGN KEY (credentials_id) REFERENCES credentials (id)
);

-- Tabla intermedia ManyToMany: Role <-> Permission
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles       (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- Tabla intermedia ManyToMany: User <-> Role
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- =============================================================================
-- INFRAESTRUCTURA: project_areas, buildings, contact_info
-- =============================================================================

CREATE TABLE IF NOT EXISTS project_areas (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN,
    deleted     BOOLEAN,
    CONSTRAINT uq_project_areas_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS contact_info (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT    NOT NULL
);

-- ElementCollection: ContactInfo.email
CREATE TABLE IF NOT EXISTS contact_info_emails (
    contact_info_id BIGINT      NOT NULL,
    email           VARCHAR(100),
    CONSTRAINT fk_contact_info_emails_ci FOREIGN KEY (contact_info_id) REFERENCES contact_info (id)
);

-- ElementCollection: ContactInfo.phoneNumber
CREATE TABLE IF NOT EXISTS contact_info_phones (
    contact_info_id BIGINT     NOT NULL,
    phone_number    VARCHAR(30),
    CONSTRAINT fk_contact_info_phones_ci FOREIGN KEY (contact_info_id) REFERENCES contact_info (id)
);

CREATE TABLE IF NOT EXISTS buildings (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    street          VARCHAR(200) NOT NULL,
    number          INTEGER      NOT NULL,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    zip_code        VARCHAR(20)  NOT NULL,
    building_type   VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    project_area_id BIGINT,
    CONSTRAINT uq_buildings_tenant_code   UNIQUE (tenant_id, code),
    CONSTRAINT fk_buildings_project_area  FOREIGN KEY (project_area_id) REFERENCES project_areas (id)
);

-- =============================================================================
-- PROVEEDORES: suppliers
-- =============================================================================

CREATE TABLE IF NOT EXISTS suppliers (
    id                          BIGSERIAL      PRIMARY KEY,
    tenant_id                   BIGINT         NOT NULL,
    cuit                        VARCHAR(255),
    legal_name                  VARCHAR(255)   NOT NULL,
    trade_name                  VARCHAR(255),
    street                      VARCHAR(200)   NOT NULL,
    number                      INTEGER        NOT NULL,
    city                        VARCHAR(100)   NOT NULL,
    state                       VARCHAR(100)   NOT NULL,
    country                     VARCHAR(100)   NOT NULL,
    zip_code                    VARCHAR(20)    NOT NULL,
    contact_info_id             BIGINT,
    pending_balance             NUMERIC(19, 2),
    default_discount_percentage NUMERIC(19, 2),
    comment                     VARCHAR(500),
    active                      BOOLEAN        NOT NULL,
    deleted                     BOOLEAN        NOT NULL,
    CONSTRAINT uq_suppliers_tenant_cuit       UNIQUE (tenant_id, cuit),
    CONSTRAINT uq_suppliers_tenant_legal_name UNIQUE (tenant_id, legal_name),
    CONSTRAINT fk_suppliers_contact_info      FOREIGN KEY (contact_info_id) REFERENCES contact_info (id)
);

-- ElementCollection: Supplier.allowedPaymentMethods
CREATE TABLE IF NOT EXISTS supplier_allowed_payment_methods (
    supplier_id    BIGINT      NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    CONSTRAINT fk_supplier_payment_methods FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- =============================================================================
-- DOCUMENTOS TRANSACCIONALES
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactional_documents (
    id                  BIGSERIAL      PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL,
    date                DATE           NOT NULL,
    document_type       VARCHAR(50)    NOT NULL,
    branch_code         VARCHAR(255)   NOT NULL,
    document_number     VARCHAR(255)   NOT NULL,
    supplier_id         BIGINT         NOT NULL,
    other_taxes         NUMERIC(19, 2) NOT NULL,
    net_total           NUMERIC(19, 2) NOT NULL,
    iva_total           NUMERIC(19, 2) NOT NULL,
    iva_exempt_total    NUMERIC(19, 2) NOT NULL,
    total               NUMERIC(19, 2) NOT NULL,
    discount_percentage NUMERIC(19, 2) NOT NULL,
    comment             VARCHAR(500),
    paid                BOOLEAN        NOT NULL,
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,
    project_area_id     BIGINT,
    CONSTRAINT uq_transactional_documents_tenant_branch_docnum_supplier
        UNIQUE (tenant_id, branch_code, document_number, supplier_id),
    CONSTRAINT fk_transactional_documents_supplier     FOREIGN KEY (supplier_id)     REFERENCES suppliers     (id),
    CONSTRAINT fk_transactional_documents_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas (id)
);

CREATE TABLE IF NOT EXISTS items (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS item_details (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      BIGINT         NOT NULL,
    item_id        BIGINT         NOT NULL,
    document_id    BIGINT         NOT NULL,
    unit_amount    NUMERIC(19, 2) NOT NULL,
    quantity       INTEGER        NOT NULL,
    iva_percentage NUMERIC(5, 2)  NOT NULL,
    total_amount   NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_item_details_item     FOREIGN KEY (item_id)     REFERENCES items                   (id),
    CONSTRAINT fk_item_details_document FOREIGN KEY (document_id) REFERENCES transactional_documents (id)
);

-- =============================================================================
-- PAGOS A PROVEEDORES
-- =============================================================================

CREATE TABLE IF NOT EXISTS payment_details (
    id           BIGSERIAL      PRIMARY KEY,
    tenant_id    BIGINT         NOT NULL,
    payment_date DATE           NOT NULL,
    supplier_id  BIGINT         NOT NULL,
    amount       NUMERIC(18, 2) NOT NULL,
    comment      VARCHAR(500),
    CONSTRAINT fk_payment_details_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- ManyToMany: PaymentDetails <-> TransactionalDocument
CREATE TABLE IF NOT EXISTS payment_details_paid_documents (
    payment_details_id        BIGINT NOT NULL,
    transactional_document_id BIGINT NOT NULL,
    PRIMARY KEY (payment_details_id, transactional_document_id),
    CONSTRAINT fk_pdd_payment_details   FOREIGN KEY (payment_details_id)        REFERENCES payment_details         (id),
    CONSTRAINT fk_pdd_transact_document FOREIGN KEY (transactional_document_id) REFERENCES transactional_documents (id)
);

-- @MapsId: PK comparte valor con payment_details_id
CREATE TABLE IF NOT EXISTS cash_payments (
    id                 BIGINT  NOT NULL PRIMARY KEY,
    tenant_id          BIGINT  NOT NULL,
    payment_details_id BIGINT  NOT NULL UNIQUE,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_cash_payments_payment_details FOREIGN KEY (payment_details_id) REFERENCES payment_details (id)
);

CREATE TABLE IF NOT EXISTS transfer_payments (
    id                 BIGINT       NOT NULL PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL,
    payment_details_id BIGINT       NOT NULL UNIQUE,
    transaction_number VARCHAR(100),
    bank_name          VARCHAR(100),
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_transfer_payments_payment_details FOREIGN KEY (payment_details_id) REFERENCES payment_details (id)
);

CREATE TABLE IF NOT EXISTS check_payments (
    id                 BIGINT       NOT NULL PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL,
    payment_details_id BIGINT       NOT NULL UNIQUE,
    check_number       VARCHAR(50),
    bank_name          VARCHAR(100),
    issue_date         DATE,
    due_date           DATE,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_check_payments_payment_details FOREIGN KEY (payment_details_id) REFERENCES payment_details (id)
);

-- =============================================================================
-- STOCK
-- =============================================================================

CREATE TABLE IF NOT EXISTS stocks (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      BIGINT         NOT NULL,
    name           VARCHAR(100)   NOT NULL,
    quantity       NUMERIC(10, 2) NOT NULL,
    stock_category VARCHAR(50)    NOT NULL,
    deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
    building_id    BIGINT,
    CONSTRAINT fk_stocks_building FOREIGN KEY (building_id) REFERENCES buildings (id)
);

-- =============================================================================
-- EMPLEADOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS employees (
    id                             BIGSERIAL    PRIMARY KEY,
    tenant_id                      BIGINT       NOT NULL,
    name                           VARCHAR(255) NOT NULL,
    last_name                      VARCHAR(255) NOT NULL,
    dni                            VARCHAR(255) NOT NULL,
    cuil                           VARCHAR(255) NOT NULL,
    project_area_id                BIGINT,
    street                         VARCHAR(200) NOT NULL,
    number                         INTEGER      NOT NULL,
    city                           VARCHAR(100) NOT NULL,
    state                          VARCHAR(100) NOT NULL,
    country                        VARCHAR(100) NOT NULL,
    zip_code                       VARCHAR(20)  NOT NULL,
    birth_date                     DATE,
    phone_number                   VARCHAR(30),
    email                          VARCHAR(100),
    emergency_contact_name         VARCHAR(100),
    emergency_contact_phone        VARCHAR(30),
    emergency_contact_relationship VARCHAR(50),
    employment_type                VARCHAR(50)  NOT NULL,
    employee_status                VARCHAR(50)  NOT NULL,
    employee_role                  VARCHAR(50)  NOT NULL,
    hire_date                      DATE         NOT NULL,
    end_date                       DATE,
    deleted                        BOOLEAN      NOT NULL,
    CONSTRAINT uq_employees_tenant_dni   UNIQUE (tenant_id, dni),
    CONSTRAINT uq_employees_tenant_cuil  UNIQUE (tenant_id, cuil),
    CONSTRAINT fk_employees_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas (id)
);

CREATE TABLE IF NOT EXISTS disciplinary_actions (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    employee_id BIGINT       NOT NULL,
    action_type VARCHAR(50)  NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    action_date DATE         NOT NULL,
    end_date    DATE,
    notes       VARCHAR(1000),
    CONSTRAINT fk_disciplinary_actions_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS employee_vacations (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    employee_id  BIGINT       NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    total_days   INTEGER      NOT NULL,
    observations VARCHAR(500),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_employee_vacations_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS epp_deliveries (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    employee_id   BIGINT       NOT NULL,
    delivery_date DATE         NOT NULL,
    item_name     VARCHAR(150) NOT NULL,
    item_type     VARCHAR(100) NOT NULL,
    brand         VARCHAR(100),
    quantity      INTEGER      NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_epp_deliveries_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS salary_payments (
    id               BIGSERIAL      PRIMARY KEY,
    tenant_id        BIGINT         NOT NULL,
    employee_id      BIGINT         NOT NULL,
    salary_frequency VARCHAR(50)    NOT NULL,
    payment_date     DATE           NOT NULL,
    amount           NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_salary_payments_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- =============================================================================
-- VEHÍCULOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS vehicle_types (
    id                       BIGSERIAL   PRIMARY KEY,
    tenant_id                BIGINT      NOT NULL,
    name                     VARCHAR(60) NOT NULL,
    description              VARCHAR(100),
    requires_truck_equipment BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_vehicle_types_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS vehicles (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    license_plate       VARCHAR(255) NOT NULL,
    brand               VARCHAR(60),
    model               VARCHAR(60),
    year                INTEGER,
    color               VARCHAR(40),
    nick_name           VARCHAR(40),
    vehicle_type_id     BIGINT,
    project_area_id     BIGINT,
    stored_in           VARCHAR(100),
    vtv_expiration_date DATE,
    jurisdiction_type   VARCHAR(50),
    truck_equipment     VARCHAR(50),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_vehicles_tenant_license_plate UNIQUE (tenant_id, license_plate),
    CONSTRAINT fk_vehicles_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id),
    CONSTRAINT fk_vehicles_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas  (id)
);

CREATE TABLE IF NOT EXISTS licence_plate_payments (
    id                BIGSERIAL      PRIMARY KEY,
    tenant_id         BIGINT         NOT NULL,
    date              DATE           NOT NULL,
    vehicle_id        BIGINT         NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    year              INTEGER        NOT NULL,
    period            INTEGER        NOT NULL,
    jurisdiction_type VARCHAR(50)    NOT NULL
);

CREATE TABLE IF NOT EXISTS repairs (
    id          BIGSERIAL      PRIMARY KEY,
    tenant_id   BIGINT         NOT NULL,
    date        DATE           NOT NULL,
    vehicle_id  BIGINT         NOT NULL,
    cost        NUMERIC(19, 2),
    description TEXT,
    employee    VARCHAR(100),
    supplier_id BIGINT,
    repair_type VARCHAR(50)    NOT NULL,
    CONSTRAINT fk_repairs_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles  (id),
    CONSTRAINT fk_repairs_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- =============================================================================
-- ESTACIONES DE SERVICIO Y CARGAS DE COMBUSTIBLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS gas_stations (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT    NOT NULL,
    supplier_id BIGINT    NOT NULL,
    deleted     BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_gas_stations_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- ElementCollection: GasStation.prices (@Embeddable GasStationPrice)
CREATE TABLE IF NOT EXISTS gas_station_prices (
    gas_station_id BIGINT         NOT NULL,
    fuel_type      VARCHAR(50)    NOT NULL,
    price          NUMERIC(10, 3) NOT NULL,
    CONSTRAINT uq_gas_station_prices_station_fuel UNIQUE (gas_station_id, fuel_type),
    CONSTRAINT fk_gas_station_prices_station FOREIGN KEY (gas_station_id) REFERENCES gas_stations (id)
);

CREATE TABLE IF NOT EXISTS fuel_loads (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL,
    date            DATE           NOT NULL,
    branch_code     VARCHAR(10)    NOT NULL,
    ticket_number   VARCHAR(50)    NOT NULL,
    fuel_type       VARCHAR(50)    NOT NULL,
    liters          NUMERIC(19, 2) NOT NULL,
    price_per_liter NUMERIC(19, 2) NOT NULL,
    total_amount    NUMERIC(19, 2) NOT NULL,
    vehicle_id      BIGINT         NOT NULL,
    project_area_id BIGINT,
    gas_station_id  BIGINT,
    CONSTRAINT fk_fuel_loads_vehicle      FOREIGN KEY (vehicle_id)      REFERENCES vehicles      (id),
    CONSTRAINT fk_fuel_loads_project_area FOREIGN KEY (project_area_id) REFERENCES project_areas (id),
    CONSTRAINT fk_fuel_loads_gas_station  FOREIGN KEY (gas_station_id)  REFERENCES gas_stations  (id)
);

-- =============================================================================
-- SEGUROS
-- =============================================================================

CREATE TABLE IF NOT EXISTS insurance_policies (
    id                     BIGSERIAL      PRIMARY KEY,
    tenant_id              BIGINT         NOT NULL,
    policy_number          VARCHAR(50)    NOT NULL,
    term_number            VARCHAR(20),
    endorsement_secuence   VARCHAR(20),
    policy_type            VARCHAR(50)    NOT NULL,
    policy_status          VARCHAR(50)    NOT NULL,
    payment_frequency      VARCHAR(50),
    sum_insured            NUMERIC(15, 2),
    issue_date             DATE,
    effective_from         DATE           NOT NULL,
    effective_to           DATE           NOT NULL,
    cancellation_date      DATE,
    number_of_installments INTEGER,
    deleted                BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_insurance_policies_tenant_policy_number UNIQUE (tenant_id, policy_number)
);

CREATE TABLE IF NOT EXISTS auto_policies (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT    NOT NULL,
    insurance_policy_id BIGINT    NOT NULL UNIQUE,
    deleted             BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_auto_policies_insurance_policy FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policies (id)
);

CREATE TABLE IF NOT EXISTS policy_vehicles (
    id                     BIGSERIAL      PRIMARY KEY,
    tenant_id              BIGINT         NOT NULL,
    vehicle_id             BIGINT         NOT NULL,
    auto_policy_id         BIGINT         NOT NULL,
    sum_insured            NUMERIC(15, 2),
    effective_from         DATE           NOT NULL,
    effective_to           DATE           NOT NULL,
    cancellation_date      DATE,
    number_of_installments INTEGER,
    deleted                BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_policy_vehicles_vehicle     FOREIGN KEY (vehicle_id)     REFERENCES vehicles      (id),
    CONSTRAINT fk_policy_vehicles_auto_policy FOREIGN KEY (auto_policy_id) REFERENCES auto_policies (id)
);

-- =============================================================================
-- SERVICIOS DE PROVEEDORES
-- =============================================================================

CREATE TABLE IF NOT EXISTS service_suppliers (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT    NOT NULL,
    supplier_id BIGINT    NOT NULL,
    deleted     BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_service_suppliers_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- ElementCollection: ServiceSupplier.providedServices (EnumType.STRING)
CREATE TABLE IF NOT EXISTS service_supplier_services (
    service_supplier_id BIGINT      NOT NULL,
    service_type        VARCHAR(50) NOT NULL,
    CONSTRAINT fk_service_supplier_services_ss FOREIGN KEY (service_supplier_id) REFERENCES service_suppliers (id)
);

CREATE TABLE IF NOT EXISTS service_payments (
    id                  BIGSERIAL      PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL,
    service_supplier_id BIGINT         NOT NULL,
    building_id         BIGINT         NOT NULL,
    service_type        VARCHAR(50)    NOT NULL,
    payment_date        DATE           NOT NULL,
    amount              NUMERIC(10, 2) NOT NULL,
    reference_number    VARCHAR(100),
    comment             VARCHAR(500),
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_service_payment_reference_number  UNIQUE (tenant_id, reference_number),
    CONSTRAINT fk_service_payments_service_supplier FOREIGN KEY (service_supplier_id) REFERENCES service_suppliers (id),
    CONSTRAINT fk_service_payments_building         FOREIGN KEY (building_id)         REFERENCES buildings         (id)
);

-- =============================================================================
-- AUDITORÍA DE EXCEPCIONES
-- (ExceptionLog NO extiende TenantEntity → sin tenant_id)
-- =============================================================================

CREATE TABLE IF NOT EXISTS exception_log (
    id             BIGSERIAL    PRIMARY KEY,
    exception_name VARCHAR(255) NOT NULL,
    message        TEXT,
    path           VARCHAR(500),
    method         VARCHAR(10),
    timestamp      TIMESTAMP    NOT NULL,
    stack_trace    TEXT
);

