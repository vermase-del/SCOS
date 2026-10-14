# SCOS: Smart Community Operating System (Apartment Management System)
### Enterprise-Grade, CI/CD Enabled Base Module Application

Modernized from legacy Infor/SoftBrands HMS (ExtJS 4.2 + Monolithic Java + 1,300+ MSSQL table schema) into a cloud-native, multi-tenant residential community management platform.

---

## 1. Technology Architecture

| Layer | Technology | Key Capabilities |
|---|---|---|
| **Frontend** | Angular 18+ (Standalone Components) | Signals Reactivity, Reactive Forms, CSS Glassmorphism Design System, Functional Guards & Interceptors |
| **Backend** | Spring Boot 3.3.4 (Java 21 LTS) | Spring Data JPA, Spring Security 6 (JWT / RBAC), Multi-Tenancy (`X-Community-Id`), Flyway Migrations, OpenAPI 3.0 |
| **Database** | PostgreSQL 16+ | Multi-tenant Normalized Schema, UUID Keys, Audit Timestamps, JSONB |
| **CI/CD** | GitHub Actions & GitLab CI | Automated Maven compilation, JaCoCo test coverage, linting, Docker buildx packaging |
| **Containers** | Docker & Docker Compose | Multi-stage distroless/alpine builds, PostgreSQL 16 service, pgAdmin |

---

## 2. Directory Structure

```
scos/
├── .github/workflows/
│   ├── backend-ci.yml             # Java 21 build, test, JaCoCo, Docker packaging
│   └── frontend-ci.yml            # Node 20 build, Angular production bundle, Docker
├── .gitlab-ci.yml                 # GitLab CI enterprise pipeline definition
├── docker/
│   ├── docker-compose.yml         # Full-stack orchestrator (PostgreSQL, Backend, Frontend, pgAdmin)
│   ├── Dockerfile.backend         # Multi-stage JDK 21 -> JRE Alpine builder
│   ├── Dockerfile.frontend        # Multi-stage Node 20 -> Nginx Alpine builder
│   └── nginx.conf                 # Nginx reverse proxy with /api routing
├── backend/                       # Spring Boot 3.3+ Core Application
│   ├── pom.xml                    # Maven parent with Java 21, Security, Flyway, OpenAPI
│   └── src/
│       ├── main/java/com/scos/
│       │   ├── common/            # Base entities, ApiResponse, GlobalExceptionHandler
│       │   ├── config/            # SecurityConfig, JpaConfig, OpenApiConfig, CorsConfig
│       │   ├── security/          # JwtTokenProvider, JwtAuthenticationFilter, TenantContext
│       │   └── modules/
│       │       ├── auth/          # LoginRequest, AuthResponse, AuthService, AuthController
│       │       ├── user/          # UserEntity, RoleEntity, UserService, UserController
│       │       └── community/     # BuildingEntity, UnitEntity, BuildingInfrastructureService/Controller
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── db/migration/      # Flyway SQL migrations (V1 schema, V2 building/units, V3 seeds)
└── frontend/                      # Angular 18+ Standalone Web Application
    ├── package.json
    ├── angular.json
    └── src/
        ├── app/
        │   ├── core/              # AuthService, AuthGuard, Interceptors, Shell Layout
        │   └── features/
        │       ├── auth/          # Glassmorphism Login Component
        │       ├── dashboard/     # Operations KPI Executive Dashboard
        │       ├── building-matrix/# 2D Interactive Residential Floor Matrix Visualizer
        │       └── users/         # User & Resident Directory
        └── styles.css             # Harmonious HSL/Hex design tokens & animations
```

---

## 3. Quickstart & Execution

### Option A: Run Full Stack with Docker Compose (Recommended)

From the `scos/` directory:
```bash
docker compose -f docker/docker-compose.yml up --build -d
```

- **Frontend Portal**: `http://localhost`
- **Backend REST API**: `http://localhost:8080`
- **Swagger 3.0 API Docs**: `http://localhost:8080/swagger-ui.html`
- **pgAdmin Console**: `http://localhost:5050` (Email: `admin@scos.local` | Password: `admin`)

---

### Option B: Run Backend & Frontend Locally

#### 1. Backend (Spring Boot 3.3+)
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

#### 2. Frontend (Angular 18+)
```bash
cd frontend
npm install
npm start
```
Access the application at `http://localhost:4200`.

---

## 4. Default Seed Credentials

The Flyway migration script (`V3__seed_initial_data.sql`) automatically provisions:

- **Username**: `admin`
- **Password**: `Password@123`
- **Role**: `SUPER_ADMIN`
- **Sample Society**: `Emerald Grand Heights` (Bangalore)
- **Sample Tower**: `Tower A - Emerald Royal` (12 Floors with 48 units and pre-seeded occupancy states)

---

## 5. Core API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT bearer token |
| `POST` | `/api/v1/auth/register` | Register new resident or staff user |
| `GET` | `/api/v1/infrastructure/buildings` | List residential towers in active community |
| `GET` | `/api/v1/infrastructure/buildings/{id}/matrix` | Get 2D floor matrix with units and occupancy states |
| `PATCH` | `/api/v1/infrastructure/units/{id}/status` | Update unit status (`VACANT`, `OWNER_OCCUPIED`, `UNDER_MAINTENANCE`) |
| `GET` | `/api/v1/users` | Paginated user and resident directory |
| `PATCH` | `/api/v1/users/{id}/status` | Activate or deactivate user accounts |
