# Local Runbook - BroadleafCommerce 4.0.x on Java 21

## Prerequisites

- **JDK 21** (OpenJDK 21 recommended)
- **Maven 3.9+** (included via Maven wrapper `./mvnw`)
- **Git**

No external databases or services are required. The test suite uses HSQLDB (embedded).

## Quick Start

```bash
# Clone the repository
git clone <repo-url>
cd BroadleafCommerce

# Verify Java version
java -version
# Expected: openjdk version "21.x.x"

# Run the full build and test suite
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
```

If your default `java` is already JDK 21, you can omit the `JAVA_HOME` prefix.

## Build Commands

```bash
# Compile only (no tests)
./mvnw -DskipTests compile

# Run all tests
./mvnw test

# Full verify (compile + test + integration tests)
./mvnw verify

# Clean build
./mvnw clean test
```

## Expected Output

All 13 modules should report SUCCESS:

```
BroadleafCommerce .................................. SUCCESS
BroadleafCommerce Common Libraries ................. SUCCESS
BroadleafCommerce Core ............................. SUCCESS
BroadleafCommerce Profile .......................... SUCCESS
BroadleafCommerce Profile Web ...................... SUCCESS
BroadleafCommerce Admin ............................ SUCCESS
BroadleafCommerce Open Admin Platform .............. SUCCESS
BroadleafCommerce CMS Module ....................... SUCCESS
BroadleafCommerce Framework ........................ SUCCESS
BroadleafCommerce Framework Web .................... SUCCESS
BroadleafCommerce Admin Module ..................... SUCCESS
BroadleafCommerce Integration ...................... SUCCESS
BroadleafCommerce Admin Functional Tests ........... SUCCESS
BUILD SUCCESS
```

## Runnable Application

BroadleafCommerce 4.0.x is a **framework library**, not a standalone application. There is no runnable entrypoint (no `main` class, no WAR with embedded server). The modules produce JAR artifacts consumed by downstream commerce applications.

### Best Local Proof

The integration test module (`integration/`) serves as the best proof of correctness. It boots a full Spring application context with HSQLDB and exercises catalog, order, pricing, offer, and checkout services end-to-end.

```bash
# Run only the integration tests
./mvnw test -pl integration
```

## Module Structure

| Module | Type | Description |
|--------|------|-------------|
| `common/` | jar | Shared utilities, extension framework |
| `core/broadleaf-profile` | jar | Customer, address, phone domain |
| `core/broadleaf-profile-web` | jar | Profile web controllers |
| `core/broadleaf-framework` | jar | Core commerce: catalog, order, pricing, offers |
| `core/broadleaf-framework-web` | jar | Thymeleaf processors, web services |
| `admin/broadleaf-open-admin-platform` | jar | Admin UI framework |
| `admin/broadleaf-contentmanagement-module` | jar | CMS admin |
| `admin/broadleaf-admin-module` | jar | Commerce admin |
| `admin/broadleaf-admin-functional-tests` | jar | Geb/Spock functional test pages |
| `integration/` | jar | Integration tests (HSQLDB) |

## Troubleshooting

### Wrong Java version
```
[ERROR] Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion failed
```
Ensure `JAVA_HOME` points to JDK 21. The Maven Enforcer plugin requires Java 21+.

### Module access errors at runtime
The build includes `--add-opens` JVM flags in surefire configuration for modules required by Hibernate, Spring, and MVEL reflection. If you see `InaccessibleObjectException`, verify surefire plugin version is 3.3.1+.
