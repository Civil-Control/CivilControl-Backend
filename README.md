<div align="center">
  <a href="#-readme-in-english">🇺🇸 English</a> | 
  <a href="#-readme-en-español">🇪🇸 Español</a>
</div>

---

# 🇺🇸 README in English

<div align="center">

# CivilControl Backend API

### Enterprise Management System REST API

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Private-red?style=for-the-badge)](LICENSE)

</div>

## 📋 Overview

CivilControl Backend is a comprehensive **RESTful API** built with **Spring Boot** designed to manage enterprise operations for ESEA SA. This microservice handles complex business logic across multiple domains including human resources, fleet management, financial operations, document processing, and mechanical services.

The API follows industry-standard architectural patterns including **layered architecture**, **DTO pattern**, **repository pattern**, and implements robust security through **JWT-based authentication** and **role-based access control (RBAC)**.

## 🏗️ Architecture & Design Patterns

### Layered Architecture
```
┌─────────────────────────────────────────────┐
│           Controller Layer                   │  ← REST Endpoints
├─────────────────────────────────────────────┤
│            Service Layer                     │  ← Business Logic
├─────────────────────────────────────────────┤
│          Repository Layer                    │  ← Data Access (JPA)
├─────────────────────────────────────────────┤
│           Database Layer                     │  ← PostgreSQL 16
└─────────────────────────────────────────────┘
```

### Design Patterns Implemented
- **DTO Pattern**: Entity-DTO separation using MapStruct for object mapping
- **Repository Pattern**: Spring Data JPA repositories for data persistence
- **Dependency Injection**: IoC container for loose coupling
- **Builder Pattern**: Lombok builders for entity construction
- **Factory Pattern**: Custom exception handling and response building
- **Chain of Responsibility**: Spring Security filter chain

### Security Architecture
- **JWT (JSON Web Tokens)**: Stateless authentication mechanism
  - Access Token: 1 hour expiration
  - Refresh Token: 7 days expiration
- **RBAC (Role-Based Access Control)**: Granular permission system across 8 business modules
- **Password Encryption**: BCrypt hashing algorithm
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Security Filter Chain**: Custom authentication filters

## 🚀 Core Features

### Business Modules

#### 🏢 Company Management
- **Suppliers**: Vendor and supplier registry with complete CRUD operations
- **Items**: Product/service catalog management
- **Buildings**: Infrastructure and facilities tracking
- **Project Areas**: Organizational structure and area assignment

#### 🚗 Fleet Management (Vehicles)
- **Vehicles**: Complete vehicle registry with technical specifications
- **Fuel Load**: Fuel consumption tracking and cost analysis
- **Gas Stations**: Service station management
- **License Plate Payments**: Registration and renewal tracking
- **Insurance Policies**: Vehicle insurance management

#### 👥 Human Resources (Personal)
- **Employees**: Personnel information and job positions
- **Salary Payments**: Payroll processing and payment history
- **Disciplinary Actions**: Incident tracking and sanctions
- **EPP Delivery**: Personal protective equipment distribution
- **Employee Vacations**: Leave management and approval workflow

#### 🔧 Mechanical Services
- **Repairs**: Vehicle and equipment maintenance tracking
- **Stock**: Spare parts and supplies inventory management

#### 📄 Document Management
- **Transactional Documents**: Invoice, receipt, and document processing
- **Payments**: Financial transaction records

#### 🛠️ Service Operations
- **Service Suppliers**: External service provider management
- **Service Payments**: Service billing and payment tracking

#### 📊 Reporting
- **Dynamic Reports**: Excel and PDF export capabilities
  - Employee reports with vacation tracking
  - Vehicle maintenance history
  - Financial summaries
  - Custom date range filtering

#### 🔐 Administration
- **Users**: User account management
- **Roles**: Role definition and assignment
- **Permissions**: 8-module permission system (Services, Documents, Vehicles, Personal, Mechanic, Report, Company, Administration)

## 🛠️ Technology Stack

### Core Framework
- **Java 17** - LTS version with modern language features
- **Spring Boot 3.3.0** - Application framework
- **Spring Data JPA** - ORM and data persistence
- **Spring Security** - Authentication and authorization
- **Spring Validation** - Input validation

### Database
- **PostgreSQL 16** - Advanced relational database with full ACID compliance
- **Hibernate** - ORM implementation
- **Connection Pool** - HikariCP (default in Spring Boot)

### Security & Authentication
- **JJWT 0.12.5** - JWT implementation
  - `jjwt-api` - JWT API
  - `jjwt-impl` - JWT implementation
  - `jjwt-jackson` - JSON processing

### Documentation & Monitoring
- **SpringDoc OpenAPI 3** (v2.6.0) - API documentation (Swagger UI)
- **Spring Actuator** - Health checks and monitoring endpoints

### Object Mapping & Utilities
- **MapStruct 1.5.5** - Type-safe DTO mapping
- **Lombok 1.18.30** - Boilerplate code reduction

### Report Generation
- **Apache POI 5.2.3** - Excel file generation (.xlsx)
- **iText7 7.2.5** - PDF document generation

### DevOps & Deployment
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Maven 3** - Build automation and dependency management

## 📦 Installation & Setup

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 16+** (or use Docker Compose)
- **Docker** (optional, for containerized deployment)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd CivilControl-backend
```

### 2. Database Setup

#### Option A: Using Docker Compose (Recommended)
```bash
docker-compose up -d db
```

#### Option B: Manual PostgreSQL Setup
```sql
CREATE DATABASE civilcontrol_backend;
CREATE USER appuser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE civilcontrol_backend TO appuser;
GRANT ALL PRIVILEGES ON civilcontrol_backend.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Environment Configuration

Create a `.env.dev` file for development:
```properties
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/civilcontrol_backend?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
```

⚠️ **IMPORTANT**: For production, generate secure secrets using cryptographic tools.

### 4. Build the Project

```bash
# Using Maven Wrapper (Recommended)
./mvnw clean package -DskipTests

# Using installed Maven
mvn clean package -DskipTests
```

### 5. Run the Application

#### Development Mode
```bash
# Using Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Using Java
java -jar target/CivilControlBackEnd-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Production Mode with Docker
```bash
# Build and start all services
docker-compose up -d

# Check logs
docker-compose logs -f api
```

The API will be available at:
- **API Base URL**: `http://localhost:8081`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/v3/api-docs`
- **Health Check**: `http://localhost:8081/actuator/health`

## 📡 API Endpoints Overview

### Authentication
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - User logout

### Core Resources
All endpoints follow RESTful conventions with standard HTTP methods:

| Module | Base Path | Description |
|--------|-----------|-------------|
| Users | `/api/users` | User account management |
| Roles | `/api/roles` | Role and permission management |
| Employees | `/api/employees` | Employee registry |
| Vehicles | `/api/vehicles` | Fleet management |
| Suppliers | `/api/suppliers` | Supplier management |
| Buildings | `/api/buildings` | Facility tracking |
| Items | `/api/items` | Product catalog |
| Repairs | `/api/repairs` | Maintenance records |
| Stock | `/api/stock` | Inventory management |
| Reports | `/api/reports` | Data export (Excel/PDF) |
| Documents | `/api/transactional-documents` | Document processing |
| Payments | `/api/payments` | Payment tracking |

### Standard CRUD Operations
Each resource typically supports:
- `GET /{resource}` - List all (with pagination)
- `GET /{resource}/{id}` - Get by ID
- `POST /{resource}` - Create new
- `PUT /{resource}/{id}` - Update existing
- `DELETE /{resource}/{id}` - Delete

### Security
All endpoints (except `/api/auth/**`) require:
- Valid JWT token in `Authorization: Bearer <token>` header
- Appropriate role permissions

## 🔧 Configuration Files

### Application Profiles
- `application.properties` - Base configuration
- `application-dev.properties` - Development environment
- `application-prod.properties` - Production environment

### Key Configuration Parameters
```properties
# Server
server.port=8081
server.address=0.0.0.0

# Database (JPA)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=${JWT_SECRET}
jwt.access-token.expiration=3600000
jwt.refresh-token.expiration=604800000

# CORS
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS}
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=EmployeeServiceTest
```

## 📊 Database Schema

The application uses **Hibernate DDL auto-update** mode for development. The schema includes:

### Core Tables
- `users` - System users and authentication
- `roles` - User roles
- `permissions` - Granular permissions
- `employees` - Personnel records
- `vehicles` - Fleet registry
- `suppliers` - Vendor information
- `buildings` - Facilities
- `items` - Product catalog
- `repairs` - Maintenance records
- `stock` - Inventory
- `transactional_documents` - Document records
- `payments` - Payment transactions

### Junction Tables
- `user_roles` - Many-to-many: Users ↔ Roles
- `role_permissions` - Many-to-many: Roles ↔ Permisos

## 🐳 Docker Deployment

### Build Docker Image
```bash
docker build -t civilcontrol-backend-api .
```

### Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f api

# Rebuild and restart
docker-compose up -d --build
```

### Health Checks
Both database and API include health checks:
- **Database**: PostgreSQL pg_isready check every 5s
- **API**: Actuator health endpoint check every 30s

## 📝 Development Scripts

### PowerShell Scripts (Windows)
- `setup-dev-env.ps1` - Development environment setup
- `test_supplier_name.ps1` - Supplier name validation tests

## 🔐 Security Best Practices

1. **Never commit secrets** - Use environment variables
2. **Rotate JWT secrets** regularly in production
3. **Use HTTPS** in production environments
4. **Implement rate limiting** for authentication endpoints
5. **Regular dependency updates** for security patches
6. **Database backups** - Automated backup strategy
7. **Audit logging** - Track sensitive operations

## 📈 Performance Considerations

- **Connection Pooling**: HikariCP with optimized settings
- **Lazy Loading**: JPA lazy fetch strategies
- **Pagination**: All list endpoints support pagination
- **Indexing**: Database indexes on foreign keys and frequently queried fields
- **Caching**: Consider implementing Redis for frequent queries
- **Query Optimization**: N+1 query prevention with JOIN FETCH

## 🤝 Contributing

### Code Style
- Follow Java naming conventions
- Use Lombok for boilerplate reduction
- Write meaningful commit messages
- Include unit tests for new features
- Document public APIs with JavaDoc

### Branch Strategy
- `main` - Production-ready code
- `develop` - Development integration
- `feature/*` - New features
- `hotfix/*` - Production fixes

## 📄 License

Private project - All rights reserved by ESEA SA.

## 👥 Authors

Developed by **Andriola Maximo** for **Ricardo from ESEA SA**

## 📞 Support

For technical support or questions, contact the development team.

---

# 🇪🇸 README en Español

<div align="center">

# CivilControl Backend API

### API REST de Sistema de Gestión Empresarial

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Licencia](https://img.shields.io/badge/Licencia-Privada-red?style=for-the-badge)](LICENSE)

</div>

## 📋 Descripción General

CivilControl Backend es una **API RESTful** integral construida con **Spring Boot** diseñada para gestionar operaciones empresariales de ESEA SA. Este microservicio maneja lógica de negocio compleja a través de múltiples dominios incluyendo recursos humanos, gestión de flotas, operaciones financieras, procesamiento de documentos y servicios mecánicos.

La API sigue patrones arquitectónicos estándar de la industria incluyendo **arquitectura en capas**, **patrón DTO**, **patrón repositorio**, e implementa seguridad robusta a través de **autenticación basada en JWT** y **control de acceso basado en roles (RBAC)**.

## 🏗️ Arquitectura y Patrones de Diseño

### Arquitectura en Capas
```
┌─────────────────────────────────────────────┐
│          Capa Controlador                    │  ← Endpoints REST
├─────────────────────────────────────────────┤
│          Capa de Servicio                    │  ← Lógica de Negocio
├─────────────────────────────────────────────┤
│         Capa de Repositorio                  │  ← Acceso a Datos (JPA)
├─────────────────────────────────────────────┤
│         Capa de Base de Datos                │  ← PostgreSQL 16+
└─────────────────────────────────────────────┘
```

### Patrones de Diseño Implementados
- **Patrón DTO**: Separación Entity-DTO usando MapStruct para mapeo de objetos
- **Patrón Repositorio**: Repositorios Spring Data JPA para persistencia de datos
- **Inyección de Dependencias**: Contenedor IoC para bajo acoplamiento
- **Patrón Builder**: Builders de Lombok para construcción de entidades
- **Patrón Factory**: Manejo de excepciones personalizadas y construcción de respuestas
- **Cadena de Responsabilidad**: Cadena de filtros de Spring Security

### Arquitectura de Seguridad
- **JWT (JSON Web Tokens)**: Mecanismo de autenticación sin estado
  - Access Token: Expiración de 1 hora
  - Refresh Token: Expiración de 7 días
- **RBAC (Control de Acceso Basado en Roles)**: Sistema de permisos granular a través de 8 módulos de negocio
- **Encriptación de Contraseñas**: Algoritmo de hashing BCrypt
- **Configuración CORS**: Compartición de recursos de origen cruzado configurable
- **Cadena de Filtros de Seguridad**: Filtros de autenticación personalizados

## 🚀 Características Principales

### Módulos de Negocio

#### 🏢 Gestión de Empresa (Company)
- **Proveedores (Suppliers)**: Registro de vendedores y proveedores con operaciones CRUD completas
- **Ítems (Items)**: Gestión de catálogo de productos/servicios
- **Edificios (Buildings)**: Seguimiento de infraestructura e instalaciones
- **Áreas de Proyecto (Project Areas)**: Estructura organizacional y asignación de áreas

#### 🚗 Gestión de Flota (Vehicles)
- **Vehículos (Vehicles)**: Registro completo de vehículos con especificaciones técnicas
- **Carga de Combustible (Fuel Load)**: Seguimiento de consumo de combustible y análisis de costos
- **Estaciones de Servicio (Gas Stations)**: Gestión de estaciones de servicio
- **Pagos de Patentes (License Plate Payments)**: Seguimiento de registros y renovaciones
- **Pólizas de Seguro (Insurance Policies)**: Gestión de seguros de vehículos

#### 👥 Recursos Humanos (Personal)
- **Empleados (Employees)**: Información de personal y puestos de trabajo
- **Pagos de Salarios (Salary Payments)**: Procesamiento de nómina e historial de pagos
- **Acciones Disciplinarias (Disciplinary Actions)**: Seguimiento de incidentes y sanciones
- **Entrega de EPP (EPP Delivery)**: Distribución de equipos de protección personal
- **Vacaciones de Empleados (Employee Vacations)**: Gestión de licencias y flujo de aprobación

#### 🔧 Servicios Mecánicos (Mechanic)
- **Reparaciones (Repairs)**: Seguimiento de mantenimiento de vehículos y equipos
- **Stock (Stock)**: Gestión de inventario de repuestos y suministros

#### 📄 Gestión Documental (Documents)
- **Documentos Transaccionales (Transactional Documents)**: Procesamiento de facturas, recibos y documentos
- **Pagos (Payments)**: Registros de transacciones financieras

#### 🛠️ Operaciones de Servicios (Services)
- **Proveedores de Servicios (Service Suppliers)**: Gestión de proveedores de servicios externos
- **Pagos de Servicios (Service Payments)**: Facturación y seguimiento de pagos de servicios

#### 📊 Reportes (Report)
- **Reportes Dinámicos**: Capacidades de exportación a Excel y PDF
  - Reportes de empleados con seguimiento de vacaciones
  - Historial de mantenimiento de vehículos
  - Resúmenes financieros
  - Filtrado por rango de fechas personalizado

#### 🔐 Administración (Administration)
- **Usuarios (Users)**: Gestión de cuentas de usuario
- **Roles (Roles)**: Definición y asignación de roles
- **Permisos (Permissions)**: Sistema de permisos de 8 módulos (Services, Documents, Vehicles, Personal, Mechanic, Report, Company, Administration)

## 🛠️ Stack Tecnológico

### Framework Principal
- **Java 17** - Versión LTS con características modernas del lenguaje
- **Spring Boot 3.3.0** - Framework de aplicación
- **Spring Data JPA** - ORM y persistencia de datos
- **Spring Security** - Autenticación y autorización
- **Spring Validation** - Validación de entrada

### Base de Datos
- **PostgreSQL 16** - Base de datos relacional avanzada con cumplimiento ACID completo
- **Hibernate** - Implementación ORM
- **Connection Pool** - HikariCP (por defecto en Spring Boot)

### Seguridad y Autenticación
- **JJWT 0.12.5** - Implementación JWT
  - `jjwt-api` - API JWT
  - `jjwt-impl` - Implementación JWT
  - `jjwt-jackson` - Procesamiento JSON

### Documentación y Monitoreo
- **SpringDoc OpenAPI 3** (v2.6.0) - Documentación de API (Swagger UI)
- **Spring Actuator** - Chequeos de salud y endpoints de monitoreo

### Mapeo de Objetos y Utilidades
- **MapStruct 1.5.5** - Mapeo de DTO con seguridad de tipos
- **Lombok 1.18.30** - Reducción de código repetitivo

### Generación de Reportes
- **Apache POI 5.2.3** - Generación de archivos Excel (.xlsx)
- **iText7 7.2.5** - Generación de documentos PDF

### DevOps y Despliegue
- **Docker** - Contenedorización
- **Docker Compose** - Orquestación multi-contenedor
- **Maven 3** - Automatización de construcción y gestión de dependencias

## 📦 Instalación y Configuración

### Requisitos Previos
- **Java 17** o superior
- **Maven 3.6+**
- **PostgreSQL 16+** (o usar Docker Compose)
- **Docker** (opcional, para despliegue contenedorizado)

### 1. Clonar el Repositorio
```bash
git clone <repository-url>
cd CivilControl-backend
```

### 2. Configuración de Base de Datos

#### Opción A: Usando Docker Compose (Recomendado)
```bash
docker-compose up -d db
```

#### Opción B: Configuración Manual de PostgreSQL
```sql
CREATE DATABASE CivilControl_backend;
CREATE USER appuser WITH PASSWORD 'tu_contraseña';
GRANT ALL PRIVILEGES ON DATABASE civilcontrol_backend TO appuser;
```

### 3. Configuración de Entorno

Crear un archivo `.env.dev` para desarrollo:
```properties
# Base de Datos
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/civilcontrol_backend?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=tu_contraseña

# Configuración JWT
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
```

⚠️ **IMPORTANTE**: Para producción, generar secretos seguros usando herramientas criptográficas.

### 4. Construir el Proyecto

```bash
# Usando Maven Wrapper (Recomendado)
./mvnw clean package -DskipTests

# Usando Maven instalado
mvn clean package -DskipTests
```

### 5. Ejecutar la Aplicación

#### Modo Desarrollo
```bash
# Usando Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Usando Java
java -jar target/CivilControlBackEnd-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Modo Producción con Docker
```bash
# Construir e iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f api
```

La API estará disponible en:
- **URL Base API**: `http://localhost:8081`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Documentación API**: `http://localhost:8081/v3/api-docs`
- **Health Check**: `http://localhost:8081/actuator/health`

## 📡 Resumen de Endpoints API

### Autenticación
- `POST /api/auth/login` - Autenticación de usuario
- `POST /api/auth/refresh` - Refrescar token de acceso
- `POST /api/auth/logout` - Cierre de sesión de usuario

### Recursos Principales
Todos los endpoints siguen convenciones RESTful con métodos HTTP estándar:

| Módulo | Ruta Base | Descripción |
|--------|-----------|-------------|
| Usuarios | `/api/users` | Gestión de cuentas de usuario |
| Roles | `/api/roles` | Gestión de roles y permisos |
| Empleados | `/api/employees` | Registro de empleados |
| Vehículos | `/api/vehicles` | Gestión de flota |
| Proveedores | `/api/suppliers` | Gestión de proveedores |
| Edificios | `/api/buildings` | Seguimiento de instalaciones |
| Ítems | `/api/items` | Catálogo de productos |
| Reparaciones | `/api/repairs` | Registros de mantenimiento |
| Stock | `/api/stock` | Gestión de inventario |
| Reportes | `/api/reports` | Exportación de datos (Excel/PDF) |
| Documentos | `/api/transactional-documents` | Procesamiento de documentos |
| Pagos | `/api/payments` | Seguimiento de pagos |

### Operaciones CRUD Estándar
Cada recurso típicamente soporta:
- `GET /{recurso}` - Listar todos (con paginación)
- `GET /{recurso}/{id}` - Obtener por ID
- `POST /{recurso}` - Crear nuevo
- `PUT /{recurso}/{id}` - Actualizar existente
- `DELETE /{recurso}/{id}` - Eliminar

### Seguridad
Todos los endpoints (excepto `/api/auth/**`) requieren:
- Token JWT válido en header `Authorization: Bearer <token>`
- Permisos de rol apropiados

## 🔧 Archivos de Configuración

### Perfiles de Aplicación
- `application.properties` - Configuración base
- `application-dev.properties` - Entorno de desarrollo
- `application-prod.properties` - Entorno de producción

### Parámetros de Configuración Clave
```properties
# Servidor
server.port=8081
server.address=0.0.0.0

# Base de Datos (JPA)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=${JWT_SECRET}
jwt.access-token.expiration=3600000
jwt.refresh-token.expiration=604800000

# CORS
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS}
```

## 🧪 Testing

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar con cobertura
./mvnw test jacoco:report

# Ejecutar clase de test específica
./mvnw test -Dtest=EmployeeServiceTest
```

## 📊 Esquema de Base de Datos

La aplicación usa el modo **Hibernate DDL auto-update** para desarrollo. El esquema incluye:

### Tablas Principales
- `users` - Usuarios del sistema y autenticación
- `roles` - Roles de usuario
- `permissions` - Permisos granulares
- `employees` - Registros de personal
- `vehicles` - Registro de flota
- `suppliers` - Información de proveedores
- `buildings` - Instalaciones
- `items` - Catálogo de productos
- `repairs` - Registros de mantenimiento
- `stock` - Inventario
- `transactional_documents` - Registros de documentos
- `payments` - Transacciones de pago

### Tablas de Unión
- `user_roles` - Muchos-a-muchos: Usuarios ↔ Roles
- `role_permissions` - Muchos-a-muchos: Roles ↔ Permisos

## 🐳 Despliegue con Docker

### Construir Imagen Docker
```bash
docker build -t civilcontrol-backend-api .
```

### Ejecutar con Docker Compose
```bash
# Iniciar todos los servicios
docker-compose up -d

# Detener servicios
docker-compose down

# Ver logs
docker-compose logs -f api

# Reconstruir y reiniciar
docker-compose up -d --build
```

### Health Checks
Tanto la base de datos como la API incluyen chequeos de salud:
- **Base de Datos**: Chequeo pg_isready de PostgreSQL cada 5s
- **API**: Chequeo de endpoint de salud de Actuator cada 30s

## 📝 Scripts de Desarrollo

### Scripts PowerShell (Windows)
- `setup-dev-env.ps1` - Configuración de entorno de desarrollo
- `test_supplier_name.ps1` - Tests de validación de nombres de proveedores

## 🔐 Mejores Prácticas de Seguridad

1. **Nunca commitear secretos** - Usar variables de entorno
2. **Rotar secretos JWT** regularmente en producción
3. **Usar HTTPS** en entornos de producción
4. **Implementar rate limiting** para endpoints de autenticación
5. **Actualizaciones regulares de dependencias** para parches de seguridad
6. **Backups de base de datos** - Estrategia de backup automatizada
7. **Audit logging** - Rastrear operaciones sensibles

## 📈 Consideraciones de Rendimiento

- **Connection Pooling**: HikariCP con configuraciones optimizadas
- **Lazy Loading**: Estrategias de carga perezosa de JPA
- **Paginación**: Todos los endpoints de lista soportan paginación
- **Indexación**: Índices de base de datos en claves foráneas y campos consultados frecuentemente
- **Caching**: Considerar implementar Redis para consultas frecuentes
- **Optimización de Consultas**: Prevención de consultas N+1 con JOIN FETCH

## 🤝 Contribución

### Estilo de Código
- Seguir convenciones de nomenclatura Java
- Usar Lombok para reducir código repetitivo
- Escribir mensajes de commit significativos
- Incluir tests unitarios para nuevas características
- Documentar APIs públicas con JavaDoc

### Estrategia de Ramas
- `main` - Código listo para producción
- `develop` - Integración de desarrollo
- `feature/*` - Nuevas características
- `hotfix/*` - Correcciones de producción

## 📄 Licencia

Proyecto privado - Todos los derechos reservados por ESEA SA.

## 👥 Autores

Desarrollado por **Andriola Maximo** para **Ricardo de ESEA SA**

## 📞 Soporte

Para soporte técnico o preguntas, contactar al equipo de desarrollo.

---

<div align="center">
  
**Made with ☕ and Spring Boot**

</div>

