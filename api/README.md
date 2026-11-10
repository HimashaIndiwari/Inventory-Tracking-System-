# Inventory API (Spring Boot backend)

REST API for the Inventory Tracking System: Spring Boot 4.1.2, Java 17, MySQL 8, Firebase Authentication
(stateless bearer tokens). The database schema lives in `../database/schema.sql` and is applied manually
(see `../database/README.md`); the application never creates or changes tables (`ddl-auto: validate`).

## Prerequisites

- JDK 17, Maven 3.9+
- MySQL 8 with the `inventory_db` (development) and `inventory_test` (automated tests) databases created
  from `database/schema.sql`
- A **non-production** Firebase project and its service-account JSON, stored **outside** the repository

## Environment variables

Placeholders are in `.env.example`. Never commit real values.

| Variable | Used for |
|---|---|
| `DB_URL` | JDBC URL of `inventory_db`; must include `connectionTimeZone=UTC` |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL account for `inventory_db` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Absolute path to the Firebase service-account JSON |
| `APP_SECURITY_ADMIN_EMAILS` | Comma-separated admin emails; a user is ADMIN only if the email is listed **and** verified in Firebase |
| `APP_CORS_ALLOWED_ORIGIN` | Frontend origin (default `http://localhost:5173`) |

Tests read the test database from `api/test-db.properties` (git-ignored) or from environment variables
with the same names:

```properties
TEST_DB_URL=jdbc:mysql://localhost:3306/inventory_test?connectionTimeZone=UTC
TEST_DB_USERNAME=<mysql-user>
TEST_DB_PASSWORD=<mysql-password>
```

Save this file as **UTF-8 without BOM**. A byte-order mark makes the first key unreadable and the tests fail
with `'url' must start with "jdbc"`. The test profile refuses any database whose name does not end in `_test`
and disables Firebase (tokens are verified by a fake verifier).

## Running the application

```powershell
Set-Location .\api
$env:DB_URL = "jdbc:mysql://localhost:3306/inventory_db?connectionTimeZone=UTC"
$env:DB_USERNAME = "<mysql-user>"
$env:DB_PASSWORD = "<mysql-password>"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\outside\repo\service-account.json"
$env:APP_SECURITY_ADMIN_EMAILS = "<admin@example.com>"
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## Endpoints

All endpoints require `Authorization: Bearer <Firebase ID token>`. Errors use RFC 9457 problem bodies
(`type, title, status, detail, instance, code, fieldErrors`). Stock quantities and totals are JSON **strings**.

| Method | Path | Role |
|---|---|---|
| GET | `/api/auth/me` | any |
| GET | `/api/dashboard` | any |
| GET | `/api/products?search=&categoryId=&lowStock=&page=&size=` | any |
| GET | `/api/products/{id}` | any |
| POST / PUT / DELETE | `/api/products`, `/api/products/{id}` (DELETE archives; blocked while stock > 0) | ADMIN |
| GET | `/api/categories`, `/api/suppliers` | any |
| POST / PUT / DELETE | `/api/categories[/{id}]`, `/api/suppliers[/{id}]` | ADMIN |
| POST | `/api/stock/in`, `/api/stock/out` | any |
| POST | `/api/stock/adjust` | ADMIN (body: `productId`, signed `delta`, required `note` as the reason) |
| GET | `/api/stock/history?productId=&type=&search=&from=&to=&page=&size=` | any |

`page` is zero-based, `size` is 1–100 (default 20). `from`/`to` are ISO-8601 instants; `from` is inclusive,
`to` exclusive.

## Tests

Unit and slice tests (`*Test`, Surefire) never touch a database. Integration tests (`*IT`, Failsafe) use
`inventory_test` and come in two kinds:

- **Read-only** (`ApplicationContextIT`, `SchemaConformanceIT`): extend `ReadOnlyIntegrationSupport`.
- **Data-writing** (all other `*IT`): extend `DataWritingIntegrationSupport`, insert committed rows and
  **delete every row** of `inventory_test` before each test and after the class. Run them only with the
  student's explicit approval.

Always use `clean`: the VS Code Java extension compiles into `target/` and can leave classes that make
Maven report "Unresolved compilation problems".

PowerShell (inside `api\`; quote every `-D` argument):

| Command | Database access |
|---|---|
| `mvn clean test` | none |
| `mvn clean test "-Dtest=Product*Test"` | none |
| `mvn clean verify "-DskipUTs" "-Dit.test=ApplicationContextIT,SchemaConformanceIT"` | read-only |
| `mvn clean verify "-DskipUTs" "-Dit.test=StockServiceIT"` | **writes and deletes** `inventory_test` rows |
| `mvn clean verify` | full suite; **writes and deletes** `inventory_test` rows |

`-DskipTests` has no effect in this project; use `-DskipUTs` (Surefire) or `-DskipITs` (Failsafe).

### IntelliJ IDEA

Tests locate the schema through the system property `inventory.schemaFile` (Maven sets it). In a JUnit run
configuration either set the working directory to the repository root or `api/` (the lookup searches for
`database/schema.sql` from there), or add the VM option `-Dinventory.schemaFile=<repo>\database\schema.sql`. Alternatively run the Maven goals
above from the Maven tool window.

- `Cannot locate database/schema.sql`: wrong working directory or missing `inventory.schemaFile`.
- Unresolved `TEST_DB_URL`: `api/test-db.properties` missing (or saved with a BOM) and no environment variables.
